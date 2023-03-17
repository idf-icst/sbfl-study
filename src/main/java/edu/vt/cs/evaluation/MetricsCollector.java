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
                        .count(0)
                        .build()
        };
    }

    @Override
    public BiConsumer<OverallMetrics[], Metrics> accumulator() {
        return (acc, metrics) -> acc[0] = ImmutableOverallMetrics.builder()
                .top1(acc[0].getTop1() + metrics.getTop1())
                .top5(acc[0].getTop5() + metrics.getTop5())
                .top10(acc[0].getTop10() + metrics.getTop10())
                .map(acc[0].getMap() + metrics.getMap())
                .mrr(acc[0].getMrr() + metrics.getMrr())
                .count(acc[0].getCount() + 1)
                .build();
    }

    @Override
    public BinaryOperator<OverallMetrics[]> combiner() {
        return (acc1, acc2) -> new OverallMetrics[]{ImmutableOverallMetrics.builder()
                .top1(acc1[0].getTop1() + acc2[0].getTop1())
                .top5(acc1[0].getTop5() + acc2[0].getTop5())
                .top10(acc1[0].getTop10() + acc2[0].getTop10())
                .map(acc1[0].getMap() + acc2[0].getMap())
                .mrr(acc1[0].getMrr() + acc2[0].getMrr())
                .count(acc1[0].getCount() + acc2[0].getCount())
                .build()
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
