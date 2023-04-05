package edu.vt.cs.evaluation;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class MetricsCollector implements Collector<Metrics, OverallMetrics[], OverallMetrics[]> {
    @Override
    public Supplier<OverallMetrics[]> supplier() {
        return () -> new OverallMetrics[] {
                ImmutableOverallMetrics.builder()
                        .top1(0)
                        .top5(0)
                        .top10(0)
                        .map(0.0)
                        .mrr(0.0)
                        .top1Pct(0.0)
                        .top5Pct(0.0)
                        .top10Pct(0.0)
                        .mapPct(0.0)
                        .mrrPct(0.0)
                        .count(0)
                        .build()
        };
    }

    @Override
    public BiConsumer<OverallMetrics[], Metrics> accumulator() {

        return (acc, metrics) -> {
            int nextCount = acc[0].getCount() + 1;

            double top1Pct = (acc[0].getTop1Pct() * acc[0].getCount() + (metrics.getTop1() >= 1 ? 100.0 : 0.0)) / nextCount;
            double top5Pct = (acc[0].getTop5Pct() * acc[0].getCount() + (metrics.getTop5() >= 1 ? 100.0 : 0.0)) / nextCount;
            double top10Pct = (acc[0].getTop10Pct() * acc[0].getCount() + (metrics.getTop10() >= 1 ? 100.0 : 0.0)) / nextCount;

            double mapPct = (acc[0].getMapPct() * acc[0].getCount() + metrics.getMap() * 100) / nextCount;
            double mrrPct = (acc[0].getMrrPct() * acc[0].getCount() + metrics.getMrr() * 100) / nextCount;

            acc[0] = ImmutableOverallMetrics.builder()
                    .top1(acc[0].getTop1() + metrics.getTop1())
                    .top5(acc[0].getTop5() + metrics.getTop5())
                    .top10(acc[0].getTop10() + metrics.getTop10())
                    .map(acc[0].getMap() + metrics.getMap())
                    .mrr(acc[0].getMrr() + metrics.getMrr())
                    .top1Pct(top1Pct)
                    .top5Pct(top5Pct)
                    .top10Pct(top10Pct)
                    .mapPct(mapPct)
                    .mrrPct(mrrPct)
                    .count(nextCount)
                    .build();
        };
    }

    @Override
    public BinaryOperator<OverallMetrics[]> combiner() {
        return (acc1, acc2) -> {
            int totalCount = acc1[0].getCount() + acc2[0].getCount();

            return new OverallMetrics[]{ImmutableOverallMetrics.builder()
                    .top1(acc1[0].getTop1() + acc2[0].getTop1())
                    .top5(acc1[0].getTop5() + acc2[0].getTop5())
                    .top10(acc1[0].getTop10() + acc2[0].getTop10())
                    .map(acc1[0].getMap() + acc2[0].getMap())
                    .mrr(acc1[0].getMrr() + acc2[0].getMrr())
                    .top1Pct((acc1[0].getTop1Pct() * acc1[0].getCount() + acc2[0].getTop1Pct() * acc2[0].getCount()) / totalCount)
                    .top5Pct((acc1[0].getTop5Pct() * acc1[0].getCount() + acc2[0].getTop5Pct() * acc2[0].getCount()) / totalCount)
                    .top10Pct((acc1[0].getTop10Pct() * acc1[0].getCount() + acc2[0].getTop10Pct() * acc2[0].getCount()) / totalCount)
                    .mapPct((acc1[0].getMapPct() * acc1[0].getCount() + acc2[0].getMapPct() * acc2[0].getCount()) / totalCount)
                    .mrrPct((acc1[0].getMrrPct() * acc1[0].getCount() + acc2[0].getMrrPct() * acc2[0].getCount()) / totalCount)
                    .count(totalCount)
                    .build()
            };
        };
    }

    @Override
    public Function<OverallMetrics[], OverallMetrics[]> finisher() {
        return (acc) -> acc;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.IDENTITY_FINISH);
    }
}
