package edu.vt.cs.models;

import static edu.vt.cs.models.Constants.*;

public enum BugType {
    REAL(REAL_INPUT_BUGS_DIR, REAL_BUGS_RESUL_DIR, REAL_BUGS_SPECTRUM_DIR, CSV_RESULTS_OF_REAL_BUGS_FILE_NAME,
            REAL_TESTS_COUNT_DIR),
    ARTIFICIAL(ARTIFICIAL_INPUT_BUGS_DIR, ARTIFICIAL_BUGS_RESULT_DIR, ARTIFICIAL_BUGS_SPECTRUM_DIR,
            CSV_RESULTS_OF_ARTIFICIAL_BUGS_FILE_NAME, ARTIFICIAL_TESTS_COUNT_DIR);

    final String inputBugInfoFilePath; // where is list of all bug info
    final String outputResultDir; // where to save results
    final String tmpSpectrumDir; // where to save intermediate spectrum data
    final String csvResultsFileName; // where to save reduced csv results of bugs
    final String csvTestCountDir; // where to save reduced csv results of bugs

    BugType(String inputBugInfoFilePath, String outputResultDir, String tmpSpectrumDir,
            String csvResultsFileName, String csvTestCountDir) {
        this.inputBugInfoFilePath = inputBugInfoFilePath;
        this.outputResultDir = outputResultDir;
        this.tmpSpectrumDir = tmpSpectrumDir;
        this.csvResultsFileName = csvResultsFileName;
        this.csvTestCountDir = csvTestCountDir;
    }

    public String getInputBugInfoFilePath() {
        return inputBugInfoFilePath;
    }

    public String getOutputResultDir() {
        return outputResultDir;
    }

    public String getTmpSpectrumDir() {
        return tmpSpectrumDir;
    }

    public String getCsvResultsFileName() {
        return csvResultsFileName;
    }

    public String getCsvTestCountDir() {
        return csvTestCountDir;
    }
}
