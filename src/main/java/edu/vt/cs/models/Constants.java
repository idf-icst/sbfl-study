package edu.vt.cs.models;

public class Constants {
    // wget --recursive --no-parent --accept gzoltar-files.tar.gz http://fault-localization.cs.washington.edu/data
    public static final String src = "/Users/tdao/vt-2023/fault-localization.cs.washington.edu/coverage-data";
    public static final String dest = "data";
    public static final String GZOLT_ROOT = dest + "/gzoltars";
    public static final String MATRIX_FILE_NAME = "matrix";
    public static final String SPECTRA_FILE_NAME = "spectra";
    public static final String PASSED_SYMBOL = "+";
    public static final String FAILED_SYMBOL = "-";
    public static final String COVERED = "1";
    public static final String NOT_COVERED = "0";
    public static final String DATA_FILE_NAME = "gzoltar-files.tar.gz";
    public static final int REAL_BUG_ID_UPPER_BOUND = 1000;
    public static final int NO_OF_FILES_MAX = 1000;
    public static final String GROUND_TRUTH = "/Users/tdao/vt-2023/fault-localization-data/analysis/" +
            "pipeline-scripts/buggy-lines";
    public static final String BUG_FILE_ENDING = "buggy.lines";

    public static final String RESULT_OF_ARTIFICIAL_BUGS_CSV = "data/csv/results/artificial/artificial-bug-results.csv";
    public static final String RESULT_OF_REAL_BUGS_CSV = "data/csv/results/real/real-bug-results.csv";

    public static final String RESULT_REAL_BY_TRIGGERING_MODE_CSV = "data/csv/results/real/real_reduced_by_triggering_mode.csv";
    public static final String RESULT_ARTIFICIAL_BY_TRIGGERING_MODE_CSV = "data/csv/results/artificial/artificial_reduced_by_triggering_mode.csv";

    public static final String RESULT_REAL_BY_ALGORITHM_CSV = "data/csv/results/real/real_reduced_by_algorithm.csv";
    public static final String RESULT_ARTIFICIAL_BY_ALGORITHM_CSV = "data/csv/results/artificial/artificial_reduced_by_algorithm.csv";

    public static final String RESULT_REAL_BY_PROJECT_CSV = "data/csv/results/real/real_reduced_by_project.csv";
    public static final String RESULT_ARTIFICIAL_BY_PROJECT_CSV = "data/csv/results/artificial/artificial_reduced_by_project.csv";

    public static final String RESULT_REAL_BY_BUG_CSV = "data/csv/results/real/real_reduced_by_bug.csv";
    public static final String RESULT_ARTIFICIAL_BY_BUG_CSV = "data/csv/results/artificial/artificial_reduced_by_bug.csv";
    public static final String RESULT_FILE_TYPE = "json";

    public static final String REAL_INPUT_BUGS_DIR = "data/multi-bugs/all_real_bugs.json";
    public static final String REAL_BUGS_RESUL_DIR = "data/csv/results/real";
    public static final String REAL_BUGS_SPECTRUM_DIR = "data/spectrum/real";
    public static final String CSV_RESULTS_OF_REAL_BUGS_FILE_NAME = "real-bug-results.csv";

    public static final String ARTIFICIAL_INPUT_BUGS_DIR = "data/multi-bugs/all_artificial_bugs.json";
    public static final String ARTIFICIAL_BUGS_RESULT_DIR = "data/csv/results/artificial";
    public static final String ARTIFICIAL_BUGS_SPECTRUM_DIR = "data/spectrum/artificial";
    public static final String CSV_RESULTS_OF_ARTIFICIAL_BUGS_FILE_NAME = "artificial-bug-results.csv";

    public static final String CSV_TABLES_DIR = "data/csv/results/tables";

}
