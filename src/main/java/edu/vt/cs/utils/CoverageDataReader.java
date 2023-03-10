package edu.vt.cs.utils;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static edu.vt.cs.models.Constants.DATA_FILE_NAME;
import static edu.vt.cs.models.Constants.NO_OF_FILES_MAX;
import static edu.vt.cs.models.Constants.REAL_BUG_ID_UPPER_BOUND;

/**
 * Util class that helps read/load raw coverage data compressed in tar format
 * - Input: source gzoltar data downloaded from fault-localization.cs.washington.edu
 * - Output: real bugs decompressed and organized
 */
public class CoverageDataReader {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Predicate<String> isRealBug = bugId -> Integer.parseInt(bugId) <= REAL_BUG_ID_UPPER_BOUND;

    private static void decompressTarGzipFile(InputStream uploadedInputStream, Path target) throws IOException {

        try (BufferedInputStream bi = new BufferedInputStream(uploadedInputStream);
             GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

            ArchiveEntry entry;
            while ((entry = ti.getNextEntry()) != null) {
                Path newPath = zipSlipProtect(entry, target);

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Path parent = newPath.getParent();

                    if (parent != null && Files.notExists(parent)) {
                        Files.createDirectories(parent);
                    }

                    Files.copy(ti, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path zipSlipProtect(ArchiveEntry entry, Path targetDir) throws IOException {
        Path targetDirResolved = targetDir.resolve(entry.getName());
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad entry: " + entry.getName());
        }
        return normalizePath;
    }

    private static Map.Entry<Boolean, Path> decompress(Path sourceBugFilePath, String destPath) {
        var projectId = sourceBugFilePath.getParent().getParent().toFile().getName();
        var bugId = sourceBugFilePath.getParent().toFile().getName();
        LOG.info("processing project = {}, bug = {}", projectId, bugId);
        try {
            var is = Files.newInputStream(sourceBugFilePath);
            decompressTarGzipFile(is, Paths.get(destPath));
            LOG.info("Done project = {}, bugId = {}", projectId, bugId);
            return new AbstractMap.SimpleEntry<>(true, sourceBugFilePath);
        } catch (IOException ioException) {
            LOG.error("Failed to process file: {}", sourceBugFilePath.toFile().getName(), ioException);
        }
        return new AbstractMap.SimpleEntry<>(false, sourceBugFilePath);
    }

    public static void etlGzoltarCoverageData(String sourcePath, String destPath, int noOfFiles) throws IOException,
            InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        var tasks = Files.find(Paths.get(sourcePath), Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile()
                                && filePath.toFile().getName().equals(DATA_FILE_NAME)
                                && isRealBug.test(filePath.getParent().toFile().getName()))
                .limit(Math.min(noOfFiles, NO_OF_FILES_MAX))
                .map(bugFilePath -> (Callable<Map.Entry<Boolean, Path>>) () -> decompress(bugFilePath, destPath))
                .collect(Collectors.toList());

        LOG.info("Total real bugs found = {}", tasks.size());

        LOG.info("Start bulk processing...");

        var noFailedFiles = executorService.invokeAll(tasks, 1L, TimeUnit.HOURS).stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.error("Failed some task", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(ent -> !ent.getKey())
                .peek(ent -> LOG.error("This file {} failed!!!", ent.getValue().toFile().getName()))
                .count();

        LOG.info("Data all done transferred. Fails = {}", noFailedFiles);

        assert noFailedFiles == 0;

        executorService.shutdown();
    }
}
