package edu.vt.cs.ranking;

import edu.vt.cs.models.ImmutableEntity;
import edu.vt.cs.models.ImmutableSpectrum;
import edu.vt.cs.models.Spectrum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Ranker implements Ranking {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("ConstantConditions")
    @Override
    public Spectrum rank(RankingAlgorithm rankingAlgorithm, Spectrum spectrum) {
        LOG.info("Ranking {} in algorithm = {}", spectrum.getName(), rankingAlgorithm);
        var rankedList = spectrum.getEntities()
                .stream()
                .map(entity -> ImmutableEntity.copyOf(entity)
                        .withScore(SpVector.from(entity, spectrum).toScore(rankingAlgorithm)))
                .sorted(Comparator.comparingDouble(ImmutableEntity::getScore).reversed())
                .collect(Collectors.toList());

        return ImmutableSpectrum.copyOf(spectrum)
                .withRankingAlgorithm(rankingAlgorithm)
                .withRankedEntitiesList(rankedList);
    }

    @Override
    public List<Spectrum> rankAll(Spectrum spectrum) {
        return Stream.of(RankingAlgorithm.values())
                .map(rankingAlgorithm -> rank(rankingAlgorithm, spectrum))
                .collect(Collectors.toList());
    }
}
