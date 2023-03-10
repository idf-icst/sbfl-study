package edu.vt.cs.ranking;

import edu.vt.cs.models.Spectrum;

import java.util.List;

public interface Ranking {
    /**
     * Given the spectrum, rank its entities by a ranking algorithm
     * @param rankingAlgorithm an ranking algorithm input
     * @param spectrum spectrum of a program
     * @return ranked list of program's entities
     */
    Spectrum rank(RankingAlgorithm rankingAlgorithm, Spectrum spectrum);

    /**
     * Rank a spectrum's entities by all 25 ranking algorithms in the algorithm collection
     * @param spectrum input spectrum of a program
     * @return map algorithm -> ranked list
     */
    List<Spectrum> rankAll(Spectrum spectrum);
}
