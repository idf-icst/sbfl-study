package edu.vt.cs.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.cs.models.BugType;
import edu.vt.cs.models.ImmutableSpectrum;
import edu.vt.cs.models.ResultReducedType;
import edu.vt.cs.models.Spectrum;
import edu.vt.cs.ranking.RankingAlgorithm;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultParser {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        Stream.of(TriggeringMode.values())
                .forEach(triggeringMode -> Stream.of(BugType.values())
                        .forEach(bugType -> {
                            try {
                                extractBy(RankingAlgorithm.JACCARD, triggeringMode, bugType);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
    }

    private static void extractBy(RankingAlgorithm algorithm, TriggeringMode triggeringMode, BugType bugType)
            throws IOException {

        var sourceFile = bugType == BugType.REAL
                ? "data/csv/results/real/real-bug-results.csv"
                : "data/csv/results/artificial/artificial-bug-results.csv";

        var destDir = bugType == BugType.REAL
                ? "data/csv/results/real/"
                : "data/csv/results/artificial/";

        var allLines = Files.readAllLines(Paths.get(sourceFile));

        var csvContent = allLines.stream()
                .filter(line -> {
                    var tmp = line.split(",");
                    var byAlgorithm = algorithm == RankingAlgorithm.valueOf(tmp[3].trim());
                    var byTriggeringMode = triggeringMode == TriggeringMode.valueOf(tmp[2].trim());
                    return byAlgorithm && byTriggeringMode;
                })
                .collect(Collectors.joining("\n"));

        var fileName = String.format("%s_%s.csv", algorithm.name(), triggeringMode.name());

        Files.writeString(Paths.get(destDir, fileName), csvContent);
    }

    private static void getAllResultsAndReduce() {
        Stream.of(ResultReducedType.values())
                .forEach(resultReducedType -> resultReducedType.getIOConfigs().values()
                        .stream()
                        .flatMap(m -> m.entrySet().stream())
                        .forEach(e -> {
                            try {
                                readAndSummarizeCsvResults(e.getKey(), e.getValue(), resultReducedType.getReducer());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }));
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
                            return String.format("%s, %s", ent.getKey(), overallMetrics.toCsvLine());
                        }
                )
                .collect(Collectors.joining("\n"));

        Files.writeString(Paths.get(reducedOutputPath), StringLines);
    }

    public static Function<Spectrum, Callable<Optional<EvalResult>>> processedSpectrum = spectrum -> () -> {
//        LOG.info("[Start...] parsing results in {}", spectrum.getName());
        var metrics = spectrum.compute();
        if (metrics == null) {
            LOG.error("Failed to process result file = {} => null Metrics.", spectrum.getName());
            return Optional.empty();
        }

        return Optional.of(ImmutableEvalResult.builder()
                .project(spectrum.getProject())
                .bugId(spectrum.getBug().getBugId())
                .triggeringMode(spectrum.getTriggeringMode())
                .rankingAlgorithm(spectrum.getRankingAlgorithm())
                .metrics(metrics)
                .build());
    };

    private static Function<Path, Callable<Optional<EvalResult>>> spectrumFileToEvalResult = path -> () -> {
        try {
            LOG.info("[Start...] parsing results in {}", path.toFile().getName());

            var spectrum = objectMapper.readValue(path.toFile(), ImmutableSpectrum.class);
            return processedSpectrum.apply(spectrum).call();
        } catch (IOException e) {
            LOG.error("Failed to process result file = {}", path.toFile().getName(), e);
        }
        return Optional.empty();
    };

    /**
     * read all result file in the result directory. For each file, parse it back to
     * spectrum object, then compute the evaluation metrics, and map it to an EvalResult containing the metrics,
     * finally, write that evalResult object to a single csv line in the output csv file.
     */
    private static void parse(String resultDir, String csvPath) {

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (var resultFiles = Files.list(Paths.get(resultDir))) {

            var tasks = resultFiles.map(spectrumFileToEvalResult).toList();

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
