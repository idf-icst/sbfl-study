package edu.vt.cs.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.cs.models.BugType;
import edu.vt.cs.models.Constants;
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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ResultParser {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final RankingAlgorithm DEFAULT_ALGORITHM = RankingAlgorithm.DICE;

    private static final List<RankingAlgorithm> REPRESENTATIVE_ALGORITHMS = List.of(
            RankingAlgorithm.GOODMAN,
            RankingAlgorithm.HAMANN,
            RankingAlgorithm.EUCLID);

    private static void extractByDefaultAlgorithm(RankingAlgorithm rankingAlgorithm) {
        for (TriggeringMode triggeringMode : TriggeringMode.values()) {
            for (BugType bugType : BugType.values()) {
                extractBy(rankingAlgorithm, triggeringMode, bugType);
            }
        }
    }

    public static void extractByDefaultAlgorithm() {
        extractByDefaultAlgorithm(DEFAULT_ALGORITHM);
    }

    /**
     * Comparison between IFL_1 vs. IFL_c (both Artificial/Injected and Real bugs)
     * Default algorithm is DICE
     * Format:
     * ifl_1 | top1/I | top1/R | top5/I | top5/R | MAP/I | MAP/R | MRR/I | MRR/R
     * ifl_c | top1/I | top1/R | top5/I | top5/R | MAP/I | MAP/R | MRR/I | MRR/R
     * Where: I = Injected/Artificial Bugs; and R = Real Bugs
     */
    public static void extractTableV() throws IOException {
        var ifl1Artificial = extractBy(DEFAULT_ALGORITHM, TriggeringMode.FIRST_FAILED_TEST, BugType.ARTIFICIAL);
        var ifl1Real = extractBy(DEFAULT_ALGORITHM, TriggeringMode.FIRST_FAILED_TEST, BugType.REAL);

        var ifl1CsvLine = String.format("%s, %d, %d, %d, %d, %d, %d, %d, %d",
                TriggeringMode.FIRST_FAILED_TEST,

                Math.round(ifl1Artificial.getTop1Pct()), Math.round(ifl1Real.getTop1Pct()),

                Math.round(ifl1Artificial.getTop5Pct()), Math.round(ifl1Real.getTop5Pct()),

                Math.round(ifl1Artificial.getMapPct()), Math.round(ifl1Real.getMapPct()),

                Math.round(ifl1Artificial.getMrrPct()), Math.round(ifl1Real.getMrrPct())
        );

        var iflcArtificial = extractBy(DEFAULT_ALGORITHM, TriggeringMode.COMPLETE, BugType.ARTIFICIAL);
        var iflcReal = extractBy(DEFAULT_ALGORITHM, TriggeringMode.COMPLETE, BugType.REAL);

        var iflcCsvLine = String.format("%s, %d, %d, %d, %d, %d, %d, %d, %d",
                TriggeringMode.COMPLETE,

                Math.round(iflcArtificial.getTop1Pct()), Math.round(iflcReal.getTop1Pct()),

                Math.round(iflcArtificial.getTop5Pct()), Math.round(iflcReal.getTop5Pct()),

                Math.round(iflcArtificial.getMapPct()), Math.round(iflcReal.getMapPct()),

                Math.round(iflcArtificial.getMrrPct()), Math.round(iflcReal.getMrrPct())
        );

        var headerLine = "Triggering Mode, Top-1(I), Top-1(R), Top-5(I), Top-5(R), MAP(I), MAP(R), MRR(I), MRR(R)\n";

        var csvContent = headerLine + ifl1CsvLine + "\n" + iflcCsvLine;

        Files.writeString(Paths.get(Constants.CSV_TABLES_DIR, "Table_V.csv"), csvContent);
    }

    /**
     * Effectiveness of IFL_f (i.e., effectiveness by sequence encountering of failed tests)
     * Default ranking algorithm = DICE
     * Format:
     * #Failed_Tests | top1/I | top1/R | top5/I | top5/R | MAP/I | MAP/R | MRR/I | MRR/R
     * Where, I = Artificial/Injected Bugs, and R = Real Bugs
     */
    public static void extractTableVII() throws IOException {

        var allFailedTestTriggeringModes = Stream.of(
                TriggeringMode.FIRST_FAILED_TEST,
                TriggeringMode.SECOND_FAILED_TEST,
                TriggeringMode.THIRD_FAILED_TEST,
                TriggeringMode.FOURTH_FAILED_TEST,
                TriggeringMode.FIFTH_FAILED_TEST
        );

        var headerLine = "# of Failed Tests, Top-1(I), Top-1(R), Top-5(I), Top-5(R), MAP(I), MAP(R), MRR(I), MRR(R)\n";

        var csvContent = headerLine + byModeSequences(allFailedTestTriggeringModes);

        Files.writeString(Paths.get(Constants.CSV_TABLES_DIR, "Table_VII.csv"), csvContent);
    }

    /**
     * Effectiveness of IFL_o (i.e., effectiveness by sequence of next passing tests)
     * Default ranking algorithm = DICE
     * Format:
     * #Passed_Tests | top1/I | top1/R | top5/I | top5/R | MAP/I | MAP/R | MRR/I | MRR/R
     * Where, I = Artificial/Injected Bugs, and R = Real Bugs
     */
    public static void extractTableIX() throws IOException {

        var allPassedTestTriggeringModes = Stream.of(
                TriggeringMode.EXTRA_PASSED_TESTS_1,
                TriggeringMode.EXTRA_PASSED_TESTS_2,
                TriggeringMode.EXTRA_PASSED_TESTS_3,
                TriggeringMode.EXTRA_PASSED_TESTS_4,
                TriggeringMode.EXTRA_PASSED_TESTS_5,
                TriggeringMode.EXTRA_PASSED_TESTS_6,
                TriggeringMode.EXTRA_PASSED_TESTS_7,
                TriggeringMode.EXTRA_PASSED_TESTS_8,
                TriggeringMode.EXTRA_PASSED_TESTS_9,
                TriggeringMode.EXTRA_PASSED_TESTS_10
        );

        var headerLine = "# of Additional Passed Tests, Top-1(I), Top-1(R), Top-5(I), Top-5(R), MAP(I), MAP(R), MRR(I), MRR(R)\n";

        var csvContent = headerLine + byModeSequences(allPassedTestTriggeringModes);

        Files.writeString(Paths.get(Constants.CSV_TABLES_DIR, "Table_IX.csv"), csvContent);
    }

    private static String byModeSequences(Stream<TriggeringMode> triggeringModes) {
        AtomicInteger counter = new AtomicInteger(0);

        return triggeringModes.map(passedTestMode -> {
            var artificialBugMetrics = extractBy(DEFAULT_ALGORITHM, passedTestMode, BugType.ARTIFICIAL);
            var realBugMetrics = extractBy(DEFAULT_ALGORITHM, passedTestMode, BugType.REAL);

            return String.format("%d, %d, %d, %d, %d, %d, %d, %d, %d",
                    counter.incrementAndGet(),

                    Math.round(artificialBugMetrics.getTop1Pct()), Math.round(realBugMetrics.getTop1Pct()),

                    Math.round(artificialBugMetrics.getTop5Pct()), Math.round(realBugMetrics.getTop5Pct()),

                    Math.round(artificialBugMetrics.getMapPct()), Math.round(realBugMetrics.getMapPct()),

                    Math.round(artificialBugMetrics.getMrrPct()), Math.round(realBugMetrics.getMrrPct())
            );
        }).collect(Collectors.joining("\n"));
    }

    /**
     * Effectiveness of IFL_1 and IFL_c by all individual ranking algorithms (both Artificial/Injected and Real bugs)
     */
    public static void extractTableX() throws IOException {
        var tableXa = extractForTableXHelper(TriggeringMode.FIRST_FAILED_TEST);
        var tableXb = extractForTableXHelper(TriggeringMode.COMPLETE);
        int n = tableXa.size();


        var headerLine = "Variants of IFL_1, Top-1(I), Top-1(R), Top-5(I), Top-5(R), MAP(I), MAP(R), MRR(I), MRR(R), " +
                "Variants of IFL_c, Top-1(I), Top-1(R), Top-5(I), Top-5(R), MAP(I), MAP(R), MRR(I), MRR(R)\n";

        var csvContent = headerLine + IntStream.range(0, n).boxed()
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

    /**
     * Effectiveness by sequence of failing tests
     */
    public static void extractDataForFig3() throws IOException {
        var csvTables = extractDataForFig3And4Helper(List.of(
                TriggeringMode.FIRST_FAILED_TEST,
                TriggeringMode.SECOND_FAILED_TEST,
                TriggeringMode.THIRD_FAILED_TEST,
                TriggeringMode.FOURTH_FAILED_TEST,
                TriggeringMode.FIFTH_FAILED_TEST));

        Files.writeString(Paths.get(Constants.CSV_TABLES_DIR, "Figure_3.csv"), csvTables);
    }

    /**
     * Effectiveness by sequence of passing tests
     */
    public static void extractDataForFig4() throws IOException {
        var csvTables = extractDataForFig3And4Helper(List.of(
                TriggeringMode.EXTRA_PASSED_TESTS_1,
                TriggeringMode.EXTRA_PASSED_TESTS_2,
                TriggeringMode.EXTRA_PASSED_TESTS_3,
                TriggeringMode.EXTRA_PASSED_TESTS_4,
                TriggeringMode.EXTRA_PASSED_TESTS_5,
                TriggeringMode.EXTRA_PASSED_TESTS_6,
                TriggeringMode.EXTRA_PASSED_TESTS_7,
                TriggeringMode.EXTRA_PASSED_TESTS_8,
                TriggeringMode.EXTRA_PASSED_TESTS_9,
                TriggeringMode.EXTRA_PASSED_TESTS_10
        ));

        Files.writeString(Paths.get(Constants.CSV_TABLES_DIR, "Figure_4.csv"), csvTables);
    }


    private static String extractDataForFig3And4Helper(List<TriggeringMode> triggeringModes) {
        StringBuilder sb = new StringBuilder();

        for (BugType bugType : BugType.values()) {
            for (RankingAlgorithm rankingAlgorithm : REPRESENTATIVE_ALGORITHMS) {
                var headerLine1 = rankingAlgorithm + ",,,,\n";
                var headerLine2 = bugType + ",,,,\n";

                var csvTable = headerLine1 + headerLine2 + extractDataForFigHelper(triggeringModes,
                        rankingAlgorithm, bugType) + "\n\n";

                sb.append(csvTable);
            }
        }

        return sb.toString();
    }

    private static String extractDataForFigHelper(List<TriggeringMode> triggeringModes,
                                                   RankingAlgorithm rankingAlgorithm, BugType bugType) {

        AtomicInteger counter = new AtomicInteger(0);

        boolean isFailingMode = triggeringModes.get(0).name().contains("FAILED");

        var headerLine = String.format("# of %s Tests, Top-1, Top-5, MAP, MRR\n", isFailingMode ? "failed" : "passed");

        return headerLine + triggeringModes.stream().map(failedTestTriggeringMode -> {
            var metrics = extractBy(rankingAlgorithm, failedTestTriggeringMode, bugType);
            return String.format("%d, %d, %d, %d, %d",
                    counter.incrementAndGet(),
                    Math.round(metrics.getTop1Pct()),
                    Math.round(metrics.getTop5Pct()),
                    Math.round(metrics.getMapPct()),
                    Math.round(metrics.getMrrPct()));
        }).collect(Collectors.joining("\n"));
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

        var allMetricsResults = lines.stream()
                .map(EvalResult::parseLine)
                .collect(Collectors.groupingBy(

                        reducedType.getReducer(),

                        Collectors.mapping(EvalResult::getMetrics, new MetricsCollector())

                ));

        Comparator<Map.Entry<Object, OverallMetrics[]>> comparator = Comparator
                .<Map.Entry<Object, OverallMetrics[]>>comparingDouble(ent -> ent.getValue()[0].getTop1Pct()).reversed();

        var stringLines = allMetricsResults.entrySet()
                .stream()
                .sorted(comparator)
                .map(ent -> ent.getKey() + ", " + ent.getValue()[0].toCsvLine())
                .collect(Collectors.joining("\n"));

        stringLines = (", " + OverallMetrics.toScvHeaderLine()) + stringLines;

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
