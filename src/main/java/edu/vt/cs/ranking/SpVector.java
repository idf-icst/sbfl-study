package edu.vt.cs.ranking;

import edu.vt.cs.models.Entity;
import edu.vt.cs.models.Spectrum;

public class SpVector {
    double ef;
    double ep;
    double nf;
    double np;

    private SpVector(double ef, double ep, double nf, double np) {
        this.ef = ef;
        this.ep = ep;
        this.nf = nf;
        this.np = np;
    }

    public static SpVector from(Entity entity, Spectrum spectrum) {
        return new SpVector(entity.getNumberOfFailedTests(), entity.getNumberOfPassedTests(),
                spectrum.getTotalOfFailedTests(), spectrum.getTotalOfPassedTests());
    }

    double toScore(RankingAlgorithm rankingAlgorithm) {
        return switch (rankingAlgorithm) {
            case TARANTULA -> toTarantula();
            case AMPLE -> toAmple();
            case EUCLID -> toEuclid();
            case M1 -> toM1();
            case M2 -> toM2();
            case DICE -> toDice();
            case SOKAL -> toSokal();
            case WONG1 -> toWong1();
            case ANDERBERG -> toAnderberg();
            case GOODMAN -> toGoodman();
            case HAMANN -> toHamann();
            case JACCARD -> toJaccard();
            case KULCZYNSKI1 -> toKulczynski1();
            case KULCZYNSKI2 -> toKulczynski2();
            case OCHIAI2 -> toOchiai2();
            case ZOLTAR -> toZoltar();
            case ROGERSTANIMOTO -> toRogersTanimoto();
            case WONG3 -> toWong3();
            case HAMMING -> toHamming();
            case OCHIAI -> toOchiai();
            case OVERLAP -> toOverlap();
            case RUSSELLRAO -> toRussellRao();
            case WONG2 -> toWong2();
            case SIMPLEMATCHING -> toSimpleMatching();
            case SORENSENDICE -> toSorensenDice();
        };

    }

    public double toTarantula() {
        return Math.round(nf) == 0 ? 0 : (ef / (ef + nf)) / ((ef / (ef + nf)) + (ep / (ep + np)));
    }

    public double toOchiai() {
        return Math.round(nf) != 0 ? ef / Math.sqrt((ef + ep) * (ef + nf)) : 0;
    }

    public double toJaccard() {
        return ef / (ef + ep + nf);
    }

    public double toRussellRao() {
        return ef / (ef + ep + nf + np);
    }

    public double toSorensenDice() {
        return 2 * ef / (2 * ef + ep + nf);
    }

    public double toKulczynski1() {
        return ef / (nf + ep);
    }

    public double toSimpleMatching() {
        return (ef + np) / (ef + ep + nf + np);
    }

    public double toM1() {
        return (ef + np) / (nf + ep);
    }

    public double toRogersTanimoto() {
        return (ef + np) / (ef + np + 2 * nf + 2 * ep);
    }

    public double toHamming() {
        return ef + np;
    }

    public double toOverlap() {
        return ef / Math.min(Math.min(ef, ep), nf);
    }

    public double toOchiai2() {
        return ef * np / Math.sqrt((ef + ep) * (nf + np) * (ef + np) * (ep + nf));
    }

    public double toWong1() {
        return ef;
    }

    public double toAmple() {
        return Math.abs(ef / (ef + nf) - ep / (ep + np));
    }

    public double toHamann() {
        return (ef + np - ep - nf) / (ef + ep + nf + np);
    }

    public double toDice() {
        return 2 * ef / (ef + ep + nf);
    }

    public double toKulczynski2() {
        return (1 / 2) * (ef / (ef + nf) + ef / (ef + ep));
    }

    public double toSokal() {
        return (2 * ef + 2 * np) / (2 * ef + 2 * np + nf + ep);
    }

    public double toM2() {
        return ef / (ef + np + 2 * nf + 2 * ep);
    }

    public double toGoodman() {
        return (2 * ef - nf - ep) / (2 * ef + nf + ep);
    }

    public double toEuclid() {
        return Math.sqrt(ef + np);
    }

    public double toAnderberg() {
        return ef / (ef + 2 * ep + 2 * nf);
    }

    public double toZoltar() {
        return ef / (ef + ep + nf + 10000 * nf * ep / ef);
    }

    public double toWong2() {
        return ef - ep;
    }

    public double toWong3() {
        double h = ep;
        if (ep > 2 && ep <= 10) {
            h = 2 + 0.1 * (ep - 2);
        } else if (ep > 10) {
            h = 2.8 + 0.01 * (ep - 10);
        }
        return ef - h;
    }
}
