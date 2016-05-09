/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.johannes.studies.matrix2014.analysis;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.studies.matrix2014.matrix.VolumePredicate;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.util.Collection;
import java.util.List;

/**
 * @author jillenberger
 */
public class MatrixComparator implements AnalyzerTask<Collection<? extends Person>> {

    private static final Logger logger = Logger.getLogger(MatrixComparator.class);

    private final MatrixBuilder builder;

    private final NumericMatrix refMatrix;

    private ODPredicate<String, Double> normPredicate;

    private double volumeThreshold;

    private AnalyzerTask<Pair<NumericMatrix, NumericMatrix>> tasks;

    public MatrixComparator(NumericMatrix refMatrix, MatrixBuilder builder, AnalyzerTask<Pair<NumericMatrix, NumericMatrix>> tasks) {
        this.refMatrix = refMatrix;
        this.tasks = tasks;
        this.builder = builder;
        volumeThreshold = 0;
    }

    public void setLegPredicate(Predicate<Segment> predicate) {
        builder.setLegPredicate(predicate);
    }

    public void setUseWeights(boolean useWeights) {
        builder.setUseWeights(useWeights);
    }

    public void setNormPredicate(ODPredicate<String, Double> normPredicate) {
        this.normPredicate = normPredicate;
    }

    public void setVolumeThreshold(double threshold) {
        this.volumeThreshold = threshold;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        NumericMatrix simMatrix = builder.build(persons);
        NumericMatrix tmpSimMatrix = simMatrix;
        if (normPredicate != null) {
            tmpSimMatrix = (NumericMatrix) MatrixOperations.subMatrix(normPredicate, simMatrix, new NumericMatrix());
        }

        double simTotal = MatrixOperations.sum(tmpSimMatrix);

        NumericMatrix tmpRefMatrix = refMatrix;
        if (normPredicate != null) {
            tmpRefMatrix = (NumericMatrix) MatrixOperations.subMatrix(normPredicate, refMatrix, new NumericMatrix());
        }

        double refTotal = MatrixOperations.sum(tmpRefMatrix);

        if(volumeThreshold > 0) {
            ODPredicate volPredicate = new VolumePredicate(volumeThreshold);
            // TODO: this requires further considerations...
            tmpRefMatrix = (NumericMatrix) MatrixOperations.subMatrix(volPredicate, tmpRefMatrix, new NumericMatrix());
        }

        double normFactor = refTotal/simTotal;
        MatrixOperations.applyFactor(simMatrix, normFactor);
        logger.debug(String.format("Normalization factor: %s.", normFactor));

        tasks.analyze(new ImmutablePair<>(tmpRefMatrix, simMatrix), containers);
    }
}
