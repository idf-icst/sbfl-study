package edu.vt.cs.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.cs.models.Bug;
import edu.vt.cs.models.Constants;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.vt.cs.utils.TestSubsetUtil.divideListBySizeK;

public class Evaluator {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String bugDir = Constants.BUG_DIR;
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

    private final BiFunction<Bug, TriggeringMode, Callable<Spectrum>> toSpectrumParsingTask
            = (bug, triggeringMode) -> () -> CoverageParser.parse(bug, triggeringMode, gzoltarsDir);

    private static final Function<Spectrum, Callable<List<Spectrum>>> toRankingTasks
            = spectrum -> () -> new Ranker().rankAll(spectrum);

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

    static final int threadPoolSize = Runtime.getRuntime().availableProcessors();
    static final int k = 2;

    private void eval(List<Bug> bugs) throws InterruptedException {

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

        var results = executorService.invokeAll(rankingTasks, 7L, TimeUnit.DAYS);

        var writingResultToFileTasks = results.stream().map(Evaluator::parseFutureGet)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(toFileWritingTask)
                .map(executorService::submit)
                .toList();

        writingResultToFileTasks.forEach(Evaluator::getRunnable);

        executorService.shutdown();
    }

    public void evalAll() throws IOException {

        AtomicInteger bugsProcessed = new AtomicInteger(0);

        var allMultiLocationBugs = BugParser.derBugs(bugDir);

        var bulkLoads = divideListBySizeK(allMultiLocationBugs, k);

        Function<List<Bug>, Runnable> bugsRunnable = bugs -> () -> {
            try {
                eval(bugs);
                LOG.info("\nBugs have been processed so far: {} / {}\n",
                        bugsProcessed.addAndGet(bugs.size()), allMultiLocationBugs.size());
            } catch (InterruptedException e) {
                LOG.error("Error running these bugs ={} ", bugs, e);
            }
        };

        final ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        bulkLoads.stream().map(bugsRunnable).forEach(executorService::submit);
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
        evaluateAllAndSaveIntermediateResults(args);
    }

    private static void evaluateAllAndSaveIntermediateResults(String[] args) throws IOException {
        var startTime = LocalDateTime.now();

        String bugDir = args.length > 0 ? args[0] : null;
        String gzoltarsDir = args.length > 1 ? args[1] : null;
        String resultDir = args.length > 2 ? args[2] : null;

        Evaluator evaluator = new Evaluator(bugDir, gzoltarsDir, resultDir);

        evaluator.evalAll();

        var endTime = LocalDateTime.now();

        LOG.info("DONE! time taken = {} hours", ChronoUnit.HOURS.between(startTime, endTime));
    }
}
