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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Evaluator {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final BiFunction<Bug, TriggeringMode, Callable<Spectrum>> toSpectrumParsingTask
            = (bug, triggeringMode) -> () -> CoverageParser.parse(bug, triggeringMode);

    private static final Function<Spectrum, Callable<List<Spectrum>>> toRankingTasks
            = spectrum -> () -> new Ranker().rankAll(spectrum);

    private static final Function<Spectrum, Runnable> toFileWriteTask = spectrum -> () -> {
        Path destPath = Paths.get(Constants.RESULT_DIR, spectrum.getName() + "." + Constants.RESULT_FILE_TYPE);
        try {
            LOG.info("Writing spectrum = {} result to file", spectrum);
            String jsonContent = objectMapper.writeValueAsString(spectrum);
            Files.writeString(destPath, jsonContent, Charset.defaultCharset());
        } catch (Exception e) {
            LOG.error("Failed to write spectrum = {} result to file", spectrum, e);
        }
    };

    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static void eval(List<Bug> bugs) throws InterruptedException {

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
                .map(toFileWriteTask)
                .map(executorService::submit)
                .toList();

        writingResultToFileTasks.forEach(Evaluator::getRunnable);

        executorService.shutdown();
    }

    public static void evalAll() throws IOException, InterruptedException {
        eval(BugParser.parse());
    }

    public static void eval(int k) throws IOException, InterruptedException {
        eval(BugParser.parse().stream().limit(k).collect(Collectors.toList()));
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
        evalAll();
    }
}
