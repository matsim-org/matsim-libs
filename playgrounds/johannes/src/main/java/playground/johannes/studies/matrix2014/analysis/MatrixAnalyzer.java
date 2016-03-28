/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import gnu.trove.list.array.TDoubleArrayList;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.studies.matrix2014.gis.ActivityLocationLayer;
import playground.johannes.studies.matrix2014.matrix.DefaultMatrixBuilder;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.studies.matrix2014.matrix.VolumePredicate;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 */
public class MatrixAnalyzer implements AnalyzerTask<Collection<? extends Person>> {

    private static final Logger logger = Logger.getLogger(MatrixAnalyzer.class);

    private static final String KEY = "matrix";

    private NumericMatrix refMatrix;

    private final String matrixName;

    private final DefaultMatrixBuilder matrixBuilder;

    private Predicate<Segment> predicate;

    private FileIOContext ioContext;

    private HistogramWriter histogramWriter;

    private ODPredicate<String, Double> odPredicate;

    private double volumeThreshold = 0;

    private boolean useWeights;

//    private

    public MatrixAnalyzer(ActivityLocationLayer facilities, ZoneCollection zones, NumericMatrix refMatrix, String name, String layerName) {
        this.refMatrix = refMatrix;
        this.matrixName = name;
        matrixBuilder = new DefaultMatrixBuilder(facilities, zones);
    }

    public void setLegPredicate(Predicate<Segment> predicate) {
        this.predicate = predicate;
    }

    public void setODPredicate(ODPredicate<String, Double> odPredicate) {
        this.odPredicate = odPredicate;
    }

    public void setVolumeThreshold(double threshold) {
        this.volumeThreshold = threshold;
    }

    public void setUseWeights(boolean useWeights) {
        this.useWeights = useWeights;
    }

    public void setFileIOContext(FileIOContext ioContext) {
        this.ioContext = ioContext;
        if (ioContext != null)
            histogramWriter = new HistogramWriter(ioContext, new PassThroughDiscretizerBuilder(new LinearDiscretizer(0.05), "linear"));
        else
            histogramWriter = null;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        matrixBuilder.setUseWeights(useWeights);
        matrixBuilder.setLegPredicate(predicate);
        NumericMatrix simMatrix = matrixBuilder.build(persons);

        if (odPredicate != null) {
            NumericMatrix tmpMatrix = new NumericMatrix();
            MatrixOperations.subMatrix(odPredicate, simMatrix, tmpMatrix);
            simMatrix = tmpMatrix;
        }

        double simTotal = MatrixOperations.sum(simMatrix);

        NumericMatrix tmpRefMatrix = refMatrix;
        if (odPredicate != null) {
            tmpRefMatrix = (NumericMatrix) MatrixOperations.subMatrix(odPredicate, tmpRefMatrix, new NumericMatrix());
        }

        double refTotal = MatrixOperations.sum(tmpRefMatrix);

        if(volumeThreshold > 0) {
            ODPredicate volPredicate = new VolumePredicate(volumeThreshold);
            tmpRefMatrix = (NumericMatrix) MatrixOperations.subMatrix(volPredicate, tmpRefMatrix, new NumericMatrix());
        }

        logger.debug(String.format("Normalization factor (%s): %s.", matrixName, simTotal/refTotal));
        MatrixOperations.applyFactor(tmpRefMatrix, simTotal / refTotal);

        NumericMatrix errMatrix = new NumericMatrix();
        MatrixOperations.errorMatrix(tmpRefMatrix, simMatrix, errMatrix);

        double[] errors = org.matsim.contrib.common.collections.CollectionUtils.toNativeArray(errMatrix.values(), true, true, true);

        String name = String.format("%s.%s.err", KEY, matrixName);
        StatsContainer container = new StatsContainer(name, errors);
        containers.add(container);

        if (histogramWriter != null)
            histogramWriter.writeHistograms(errors, name);

        if (ioContext != null) {
            try {
                /*
                write scatter plot
                */
                Set<String> keys = tmpRefMatrix.keys();
                keys.addAll(simMatrix.keys());

                logger.debug(String.format("Compared %s od relations.", keys.size()));

                TDoubleArrayList refVals = new TDoubleArrayList();
                TDoubleArrayList simVals = new TDoubleArrayList();
                for (String i : keys) {
                    for (String j : keys) {
                        Double refVol = tmpRefMatrix.get(i, j);
                        Double simVol = simMatrix.get(i, j);

                        if (refVol != null || simVol != null) {
                            if (refVol == null) refVol = 0.0;
                            if (simVol == null) simVol = 0.0;
                            refVals.add(refVol);
                            simVals.add(simVol);
                        }
                    }
                }

                StatsWriter.writeScatterPlot(refVals, simVals, matrixName, "simulation", String.format
                        ("%s/matrix.%s.scatter.txt", ioContext.getPath(), matrixName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        container = new StatsContainer(name, errors);
        containers.add(container);
    }
}
