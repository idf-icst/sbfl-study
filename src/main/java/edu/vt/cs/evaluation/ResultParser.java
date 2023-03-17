package edu.vt.cs.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.cs.models.Constants;
import edu.vt.cs.models.ImmutableSpectrum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResultParser {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) throws IOException {
        readAndSummarizeCsvResults(Constants.RESULT_WITH_RANKS_CSV, Constants.RESULT_BY_TRIGGERING_MODE_CSV,
                EvalResult::getTriggeringMode);

        readAndSummarizeCsvResults(Constants.RESULT_WITH_RANKS_CSV, Constants.RESULT_BY_ALGORITHM_CSV,
                EvalResult::getRankingAlgorithm);

        readAndSummarizeCsvResults(Constants.RESULT_WITH_RANKS_CSV, Constants.RESULT_BY_PROJECT_CSV,
                EvalResult::getProject);

        readAndSummarizeCsvResults(Constants.RESULT_WITH_RANKS_CSV, Constants.RESULT_BY_BUG_CSV,
                evalResult -> evalResult.getProject() + "::" + evalResult.getBugId());
    }

    private static <T> void readAndSummarizeCsvResults(String csvPath, String reducedOutputPath,
                                                       Function<EvalResult, T> keySelector) throws IOException {
        var lines = Files.readAllLines(Paths.get(csvPath));

        var allMetricsResults = lines.stream().parallel()
                .map(EvalResult::parseLine)
                .collect(Collectors.groupingBy(

                        keySelector,

                        Collectors.mapping(EvalResult::getMetrics, new MetricsCollector())

                ));

        var StringLines = allMetricsResults.entrySet()
                .stream()
                .map(ent -> {
                    OverallMetrics overallMetrics = ent.getValue()[0];
                    return String.format("%s, %s", ent.getKey(), overallMetrics.toCsvLine()); }
                )
                .collect(Collectors.joining("\n"));

        Files.writeString(Paths.get(reducedOutputPath), StringLines);
    }

    private static void parse(String resultDir, String csvPath) {
        ObjectMapper objectMapper = new ObjectMapper();

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger totalFiles = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        Function<Path, Callable<Optional<EvalResult>>> toCallable = path -> () -> {
            try {
                LOG.info("[Start...] parsing results in {}", path.toFile().getName());

                var spectrum = objectMapper.readValue(path.toFile(), ImmutableSpectrum.class);
                var metrics = spectrum.compute();
                if (metrics == null) {
                    LOG.error("Failed to process result file = {} => null Metrics.", path.toFile().getName());
                    return Optional.empty();
                }

                LOG.info("[Done] Parsing results in {}. Total: {}", path.toFile().getName(), counter.incrementAndGet());

                LOG.info("Total: {} / {}", counter.get(), totalFiles);

                return Optional.of(ImmutableEvalResult.builder()
                        .project(spectrum.getProject())
                        .bugId(spectrum.getBug().getBugId())
                        .triggeringMode(spectrum.getTriggeringMode())
                        .rankingAlgorithm(spectrum.getRankingAlgorithm())
                        .metrics(metrics)
                        .build());

            } catch (IOException e) {
                LOG.error("Failed to process result file = {}", path.toFile().getName(), e);
            }
            return Optional.empty();
        };

        try (var resultFiles = Files.list(Paths.get(resultDir))) {

            var tasks = resultFiles.map(toCallable).toList();

            totalFiles.set(tasks.size() + 1);

            var csvContent = executorService.invokeAll(tasks)
                    .stream()
                    .map(f -> {
                        try {
                            return f.get(1, TimeUnit.HOURS);
                        } catch (Exception e) {
                            LOG.error("Failed to reduce results concurrently.", e);
                        }
                        return Optional.<EvalResult>empty();
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(EvalResult::getCsvFormat)
                    .collect(Collectors.joining("\n"));

            Files.writeString(Paths.get(csvPath), csvContent);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }
}
