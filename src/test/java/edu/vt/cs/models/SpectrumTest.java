package edu.vt.cs.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class SpectrumTest {

    @Test
    void compute() throws IOException {
        var spectrumPath = Paths.get("data/results/Chart::19::COMPLETE::GOODMAN.json");
        ObjectMapper objectMapper = new ObjectMapper();
        var spectrum = objectMapper.readValue(spectrumPath.toFile(), ImmutableSpectrum.class);
        var metrics = spectrum.compute();

        assertEquals(1, metrics.getTop1());
        assertEquals(2, metrics.getTop5());
        assertEquals(2, metrics.getTop10());
        assertEquals(1, metrics.getMap());
        assertEquals(1, metrics.getMrr());
    }
}
