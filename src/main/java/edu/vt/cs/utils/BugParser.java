package edu.vt.cs.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.cs.models.Bug;
import edu.vt.cs.models.Constants;
import edu.vt.cs.models.ImmutableBug;
import edu.vt.cs.models.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.vt.cs.models.Constants.*;

public class BugParser {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Function<Path, List<String>> getBugLocations = bugFilePath -> {
        try {
            return Files.readAllLines(bugFilePath);
        } catch (Exception e) {
            LOG.error("Failed to read this file = {}", bugFilePath, e);
        }
        return List.of();
    };

    private static final Function<Path, String> getBugName = bugFilePath -> bugFilePath.toFile().getName().split("\\.")[0];

    private static final Predicate<Path> isMultiLocationBug = bugFilePath -> getBugLocations.apply(bugFilePath).size() > 1;

    private static final Function<Path, Integer> toBugId = filePath -> Integer.parseInt(filePath.toFile().getName()
            .split("-")[1].split("\\.")[0]);

    public static final Predicate<Path> isRealBugFromDefect4JProjects = filePath -> filePath.toFile().getName().endsWith(BUG_FILE_ENDING)
            && Stream.of(Project.values()).anyMatch(project -> filePath.toFile().getName().toUpperCase().startsWith(project.name().toUpperCase()))
            && toBugId.apply(filePath) < REAL_BUG_ID_UPPER_BOUND;

    private static final Predicate<Path> isArtificialBugFromDefect4JProjects = filePath -> filePath.toFile().getName().endsWith(BUG_FILE_ENDING)
            && Stream.of(Project.values()).anyMatch(project -> filePath.toFile().getName().toUpperCase().startsWith(project.name().toUpperCase()))
            && toBugId.apply(filePath) >= REAL_BUG_ID_UPPER_BOUND;

    public static Map<String, List<String>> getGroundTruths(Predicate<Path> filter) throws IOException {
        return Files.find(Paths.get(Constants.GROUND_TRUTH), Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile() && filter.test(filePath))
                .filter(isMultiLocationBug)
                .collect(Collectors.toMap(getBugName, getBugLocations));
    }

    public static List<Bug> parse(Predicate<Path> filter) throws IOException {
        List<Bug> tmp = getGroundTruths(filter).entrySet().stream()
                .map(bugEntry -> {
                    Project project = Project.valueOf(bugEntry.getKey().split("-")[0].trim());
                    int bugId = Integer.parseInt(bugEntry.getKey().split("-")[1].trim());
                    return ImmutableBug.builder()
                            .bugId(bugId)
                            .project(project)
                            .locations(bugEntry.getValue())
                            .build();
                })
                .peek(b -> LOG.info("Parsing bug = {}", b))
                .collect(Collectors.toList());

        LOG.info("Found {} multiple-location bugs", tmp.size());
        return tmp;
    }

    public static void serializeBugs(String destPath, Predicate<Path> filter) throws IOException {
        Files.writeString(Paths.get(destPath), objectMapper.writeValueAsString(parse(filter)), Charset.defaultCharset());
    }

    public static List<Bug> derBugs(String srcPath) throws IOException {
        return objectMapper.readValue(Paths.get(srcPath).toFile(), new TypeReference<>() { });
    }

    public static void exportGroundTruthsToFile(String filePath, Predicate<Path> filter) throws IOException {
        var bugToLocationsMap = getGroundTruths(filter);

        List<String> lines = bugToLocationsMap.entrySet().stream()
                .flatMap(ent -> Stream.concat(
                        ent.getValue().stream()
                                .map(bugLocation -> ent.getKey() + "::" + bugLocation),
                        Stream.of("\n")))
                .collect(Collectors.toList());

        lines.add("\nTotal = " + bugToLocationsMap.entrySet().size());

        Files.write(Paths.get(filePath), lines);
    }

    private static void printAllMultiLocationBugs() throws IOException {
        var bugToLocationsMap = getGroundTruths(isRealBugFromDefect4JProjects);

        bugToLocationsMap.forEach((key, value) -> {
            System.out.println("BugId = " + key);
            value.forEach(line -> System.out.println("\t " + line));
            System.out.println();
        });

        System.out.println("Total = " + bugToLocationsMap.entrySet().size());
    }

    public static void main(String[] args) throws IOException {
        serializeBugs(ARTIFICIAL_BUG_DIR, isArtificialBugFromDefect4JProjects);
    }
}
