package edu.vt.cs.models;

public class Constants {
    // wget --recursive --no-parent --accept gzoltar-files.tar.gz http://fault-localization.cs.washington.edu/data
    public static final String src = "/Users/tdao/vt-2023/fault-localization.cs.washington.edu/coverage-data";
    public static final String dest = "src/main/resources";
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
}
