package edu.vt.cs.evaluation;

import java.io.IOException;

public class Experiment {
    public static void main(String[] args) throws IOException, InterruptedException {
        // run main evaluation
        Evaluator.evaluateAll();

        // extract and reduce some overview/highlight info
        ResultParser.getAllResultsAndReduce();
        ResultParser.extractByDefaultAlgorithm();

        // extract tables according to the existing paper
        ResultParser.extractTableV();
        ResultParser.extractTableVII();
        ResultParser.extractTableIX();
        ResultParser.extractTableX();

        // extract data for figures according to the existing paper
        ResultParser.extractDataForFig3();
        ResultParser.extractDataForFig4();

        System.out.println("Done!");
    }
}
