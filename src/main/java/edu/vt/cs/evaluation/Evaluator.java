package edu.vt.cs.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.cs.models.Bug;
import edu.vt.cs.models.Constants;
import edu.vt.cs.models.Project;
import edu.vt.cs.models.Spectrum;
import edu.vt.cs.ranking.Ranker;
import edu.vt.cs.utils.BugParser;
import edu.vt.cs.utils.CoverageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.vt.cs.evaluation.ResultParser.processedSpectrum;
import static edu.vt.cs.utils.TestSubsetUtil.divideListBySizeK;

public class Evaluator {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper objectMapper = new ObjectMapper();

//    private String bugDir = Constants.BUG_DIR;
    private String bugDir = Constants.ARTIFICIAL_BUG_DIR;
    private String gzoltarsDir = Constants.GZOLT_ROOT;
    private String resultDir = Constants.RESULT_DIR;

    public Evaluator(String bugDir, String gzoltarsDir, String resultDir) {
        if (bugDir != null) {
            this.bugDir = bugDir;
        }

        if (gzoltarsDir != null) {
            this.gzoltarsDir = gzoltarsDir;
        }

        if (resultDir != null) {
            this.resultDir = resultDir;
        }
    }

    /**
     * [1] given a bug and a triggering mode, build a corresponding spectrum snapshot
     */
    private final BiFunction<Bug, TriggeringMode, Callable<Spectrum>> toSpectrumParsingTask
            = (bug, triggeringMode) -> () -> CoverageParser.parse(bug, triggeringMode, gzoltarsDir);

    /**
     * [2] do a ranking on a spectrum's entities, using all algorithms
     */
    private static final Function<Spectrum, Callable<List<Spectrum>>> toRankingTasks
            = spectrum -> () -> new Ranker().rankAll(spectrum);

    /**
     * [3] write a spectrum with ranked list of entities to a file
     */
    private final Function<Spectrum, Runnable> toFileWritingTask = spectrum -> () -> {
        Path destPath = Paths.get(resultDir, spectrum.getName() + "." + Constants.RESULT_FILE_TYPE);
        try {
            LOG.info("Writing spectrum = {} result to file", spectrum.getName());
            String jsonContent = objectMapper.writeValueAsString(spectrum);
            Files.writeString(destPath, jsonContent, Charset.defaultCharset());
        } catch (Exception e) {
            LOG.error("Failed to write spectrum = {} result to file", spectrum.getName(), e);
        }
    };

    private final Function<Spectrum, Callable<Optional<EvalResult>>> spectrumToEvalResult = spectrum -> () -> {
        try {
            return processedSpectrum.apply(spectrum).call();
        } catch (Exception e) {
            LOG.error("Error while converting spectrum to eval results");
        }
        return Optional.empty();
    };

    static final int threadPoolSize = Runtime.getRuntime().availableProcessors();
    static final int k = 4;

    /**
     * Given a list of bugs, process them according to this workflow:
     * [1] map each bug to spectrum of all triggering modes (1 bug => 16 spectrums, because of 16 triggering modes)
     * [2] rank entities in a spectrum using all algorithms (each spectrum => 25 ranked lists, because of 25 algorithms)
     * [3] write some 16 * 25 processed spectrums to files for later calculate metrics
     * @param bugs: a set of bugs
     * @throws InterruptedException
     */
    private String eval(List<Bug> bugs, boolean writeIntermediateResults) throws InterruptedException {

        final ExecutorService executorService = Executors.newFixedThreadPool(k);

        var parsingToSpectrumTasks = bugs
                .stream()
                .sorted(Comparator.comparing(Bug::getProject).thenComparing(Bug::getBugId))
                .flatMap(bug -> Stream.of(TriggeringMode.values())
                        .map(triggeringMode -> toSpectrumParsingTask.apply(bug, triggeringMode)))
                .collect(Collectors.toList());

        List<Future<Spectrum>> spectrumFutureList = executorService.invokeAll(parsingToSpectrumTasks, 7L, TimeUnit.DAYS);

        var rankingTasks = spectrumFutureList.stream()
                .map(Evaluator::parseFutureGet)
                .filter(Objects::nonNull)
                .map(toRankingTasks)
                .collect(Collectors.toList());

        var processedSpectrums = executorService.invokeAll(rankingTasks, 7L, TimeUnit.DAYS)
                .stream()
                .map(Evaluator::parseFutureGet)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream).toList();

        if (writeIntermediateResults) {
            var writingResultToFileTasks = processedSpectrums.stream()
                    .map(toFileWritingTask)
                    .map(executorService::submit)
                    .toList();

            writingResultToFileTasks.forEach(Evaluator::getRunnable);
        }

        return processedSpectrums.stream()
                .map(spectrumToEvalResult)
                .map(executorService::submit)
                .map(Evaluator::parseFutureGet)
                .filter(Objects::nonNull)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(EvalResult::getCsvFormat)
                .collect(Collectors.joining("\n"));

    }

    private static synchronized void saveCSVResults(List<String> csvLines) {
        var destFilePath= Paths.get("data/csv/results", "artificial-bug-results.csv");
        String csvContent = String.join("\n", csvLines);
        try {
            Files.writeString(destFilePath, csvContent);
        } catch (Exception e) {
            LOG.error("Error writing to file {}", csvContent);
        }
    }

    public void evalAll(boolean writeToIntermediateFiles, Predicate<Bug> filter) throws IOException, InterruptedException {

        var startTime = LocalDateTime.now();

        AtomicInteger bugsProcessed = new AtomicInteger(0);

        var allMultiLocationBugs = BugParser.derBugs(bugDir)
                .stream()
                .filter(filter)
                .collect(Collectors.toList());

        var bulkLoads = divideListBySizeK(allMultiLocationBugs, k);

        Function<List<Bug>, Callable<Optional<String>>> bugsRunnable = bugs -> () -> {
            try {
                var tmp = eval(bugs, writeToIntermediateFiles);
                LOG.info("\nBugs have been processed so far: {} / {}\n",
                        bugsProcessed.addAndGet(bugs.size()), allMultiLocationBugs.size());
                return Optional.of(tmp);
            } catch (InterruptedException e) {
                LOG.error("Error running these bugs ={} ", bugs, e);
            }
            return Optional.empty();
        };

        final ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        var tasks = bulkLoads.stream().map(bugsRunnable).toList();

        var csvLines = executorService.invokeAll(tasks, 1L, TimeUnit.HOURS)
                .stream()
                .map(Evaluator::parseFutureGet)
                .filter(Objects::nonNull)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        LOG.info("We got back: {} csv lines", csvLines.size());

        saveCSVResults(csvLines);

        var endTime = LocalDateTime.now();

        LOG.info("DONE! time taken = {} SECONDS", ChronoUnit.SECONDS.between(startTime, endTime));

        executorService.shutdown();
    }

    private static <V> V parseFutureGet(Future<V> f) {
        try {
            return f.get(7L, TimeUnit.DAYS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Failed to process spectrum due to concurrent issue: ", e);
        } catch (Exception e) {
            LOG.error("Failed to process spectrum, unknown issue: ", e);
        }
        return null;
    }

    private static void getRunnable(Future<?> f) {
        try {
            f.get(7L, TimeUnit.DAYS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Failed to process spectrum due to concurrent issue: ", e);
        } catch (Exception e) {
            LOG.error("Failed to process spectrum, unknown issue: ", e);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        evaluateAllAndSaveIntermediateResults(args, false);
    }

    private static void evaluateAllAndSaveIntermediateResults(String[] args,
                                                              boolean writeToIntermediateFiles) throws IOException, InterruptedException {
        String bugDir = args.length > 0 ? args[0] : null;
        String gzoltarsDir = args.length > 1 ? args[1] : null;
        String resultDir = args.length > 2 ? args[2] : null;

        Evaluator evaluator = new Evaluator(bugDir, gzoltarsDir, resultDir);

        evaluator.evalAll(writeToIntermediateFiles, bug -> bug.getProject() != Project.Closure);
    }
}
