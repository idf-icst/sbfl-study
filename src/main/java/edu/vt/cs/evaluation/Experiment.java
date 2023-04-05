package edu.vt.cs.evaluation;

import java.io.IOException;

public class Experiment {
    public static void main(String[] args) throws IOException, InterruptedException {
        Evaluator.evaluateAll();
        ResultParser.getAllResultsAndReduce();
        ResultParser.extractByDefaultAlgorithm();
        ResultParser.extractForTableX();
    }
}
