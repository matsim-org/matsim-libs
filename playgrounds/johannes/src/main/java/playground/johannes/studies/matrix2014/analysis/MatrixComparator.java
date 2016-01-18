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

import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.studies.matrix2014.matrix.MatrixBuilder;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.studies.matrix2014.matrix.VolumePredicate;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.ZoneCollection;
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

    private final String refMatrixName;

    private Predicate<Segment> legPredicate;

    private ODPredicate<String, Double> normPredicate;

    private double volumeThreshold;

    private boolean useWeights;

    private FileIOContext ioContext;

    private AnalyzerTaskComposite<NumericMatrix> tasks;

    public MatrixComparator(NumericMatrix refMatrix, String refMatrixName, ActivityFacilities facilities, ZoneCollection zones) {
        this.refMatrix = refMatrix;
        this.refMatrixName = refMatrixName;
        builder = new MatrixBuilder(facilities, zones);
        volumeThreshold = 0;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        NumericMatrix simMatrix = builder.build(persons, legPredicate, useWeights);
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
            tmpRefMatrix = (NumericMatrix) MatrixOperations.subMatrix(volPredicate, refMatrix, new NumericMatrix());
        }

        double normFactor = refTotal/simTotal;
        MatrixOperations.applyFactor(simMatrix, normFactor);
        logger.debug(String.format("Normalization factor: %s.", normFactor));


        MatrixVolumeCompare volTask = new MatrixVolumeCompare(tmpRefMatrix, String.format("matrix.%s.vol", refMatrixName));
        MatrixDistanceCompare distTask = new MatrixDistanceCompare(tmpRefMatrix, String.format("matrix.%s.dist", refMatrixName), null);

        if(ioContext != null) {
            HistogramWriter writer = new HistogramWriter(ioContext,
                    new PassThroughDiscretizerBuilder(new LinearDiscretizer(0.5), "linear"));

            volTask.setIoContext(ioContext);
            volTask.setHistogramWriter(writer);
        }

        tasks = new AnalyzerTaskComposite<>();
        tasks.addComponent(volTask);
    }
}
