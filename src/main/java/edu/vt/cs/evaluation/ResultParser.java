package edu.vt.cs.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.cs.models.*;
import edu.vt.cs.ranking.RankingAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ResultParser {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static void extractByDefaultAlgorithm(RankingAlgorithm rankingAlgorithm) throws IOException {
        for (TriggeringMode triggeringMode : TriggeringMode.values()) {
            for (BugType bugType : BugType.values()) {
                extractBy(rankingAlgorithm, triggeringMode, bugType);
            }
        }
    }

    public static void extractByDefaultAlgorithm() throws IOException {
        extractByDefaultAlgorithm(RankingAlgorithm.JACCARD);
    }

    public static void extractForTableX() throws IOException {
        var tableXa = extractForTableXHelper(TriggeringMode.FIRST_FAILED_TEST);
        var tableXb = extractForTableXHelper(TriggeringMode.COMPLETE);
        int n = tableXa.size();

        String csvContent = IntStream.range(0, n).boxed()
                .map(i -> tableXa.get(i) + ", " + tableXb.get(i))
                .collect(Collectors.joining("\n"));

        Files.writeString(Paths.get(Constants.CSV_TABLES_DIR, "Table_X.csv"), csvContent);
    }

    private static List<String> extractForTableXHelper(TriggeringMode triggeringMode) {
        return Stream.of(RankingAlgorithm.values()).sorted(Comparator.comparing(RankingAlgorithm::name))
                .map(rankingAlgorithm -> {
                    List<double[]> tmp = Stream.of(BugType.ARTIFICIAL, BugType.REAL)
                            .map(bugType -> {
                                var artOverallMetric = extractBy(rankingAlgorithm, triggeringMode, bugType);
                                return new double[]{
                                        Math.round(artOverallMetric.getTop1Pct()),
                                        Math.round(artOverallMetric.getTop5Pct()),
                                        Math.round(artOverallMetric.getMapPct()),
                                        Math.round(artOverallMetric.getMrrPct()),
                                };
                            }).toList();

                    List<String> combinedMetrics = new ArrayList<>();

                    for (int j=0; j<4; j++) {
                        for (int i=0; i<2; i++) {
                            combinedMetrics.add(String.valueOf(tmp.get(i)[j]));
                        }
                    }

                    String metricLine = String.join(", ", combinedMetrics);

                    String header = String.format("%s-%s", triggeringMode, rankingAlgorithm);

                    return header + ", " + metricLine;

                }).toList();
    }

    private static OverallMetrics extractBy(RankingAlgorithm algorithm, TriggeringMode triggeringMode, BugType bugType) {
        List<String> allLines;
        try {
            allLines = Files.readAllLines(Paths.get(bugType.getOutputResultDir(),
                    bugType.getCsvResultsFileName()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Collector<EvalResult, ?, String> cl1 = Collectors.mapping(EvalResult::getCsvFormat, Collectors.joining("\n"));
        Collector<EvalResult, ?, OverallMetrics[]> cl2 = Collectors.mapping(EvalResult::getMetrics, new MetricsCollector());

        return allLines.stream()
                .map(EvalResult::parseLine)
                .filter(evalResult -> evalResult.getRankingAlgorithm() == algorithm
                        && evalResult.getTriggeringMode() == triggeringMode)
                .collect(Collectors.teeing(cl1, cl2,
                        (s, overallMetrics) -> {
                            String csvContent = s + "\n\n" + OverallMetrics.toScvHeaderLine() + overallMetrics[0].toCsvLine();
                            var fileName = String.format("%s_%s.csv", algorithm.name(), triggeringMode.name());
                            try {
                                Files.writeString(Paths.get(bugType.getOutputResultDir(), fileName), csvContent);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return overallMetrics[0];
                }));
    }

    public static void getAllResultsAndReduce() {
        for (ResultReducedType resultReducedType : ResultReducedType.values()) {
            for (BugType bugType : BugType.values()) {
                try {
                    readAndSummarizeCsvResults(bugType, resultReducedType);
                } catch (Exception e) {
                    LOG.error("Failed to reduce results by {} of type {}", resultReducedType, bugType, e);
                }
            }
        }
    }

    private static void readAndSummarizeCsvResults(BugType bugType, ResultReducedType reducedType)
            throws IOException {

        var lines = Files.readAllLines(Paths.get(bugType.getOutputResultDir(),
                bugType.getCsvResultsFileName()));

        var allMetricsResults = lines.stream().parallel()
                .map(EvalResult::parseLine)
                .collect(Collectors.groupingBy(

                        reducedType.getReducer(),

                        Collectors.mapping(EvalResult::getMetrics, new MetricsCollector())

                ));

        Comparator<Map.Entry<Object, OverallMetrics[]>> comparator = Comparator
                .<Map.Entry<Object, OverallMetrics[]>>comparingDouble(ent -> ent.getValue()[0].getTop1Pct()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getMapPct()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getMrrPct()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getTop5Pct()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getTop10Pct()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getTop1()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getMap()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getMrr()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getTop5()).reversed()
                .thenComparingDouble(ent -> ent.getValue()[0].getTop10()).reversed();

        var stringLines = allMetricsResults.entrySet()
                .stream()
                .sorted(comparator)
                .map(ent -> ent.getKey() + ", " + ent.getValue()[0].toCsvLine())
                .collect(Collectors.joining("\n"));

        stringLines = OverallMetrics.toScvHeaderLine() + stringLines;

        String reducedFileName = String.format("%s_REDUCED_%s.csv", bugType, reducedType);

        Files.writeString(Paths.get(bugType.getOutputResultDir(), reducedFileName), stringLines);
    }

    public static Function<Spectrum, Callable<Optional<EvalResult>>> processedSpectrum = spectrum -> () -> {
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
