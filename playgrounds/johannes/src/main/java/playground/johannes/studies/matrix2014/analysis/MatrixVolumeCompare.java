/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.HistogramWriter;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 */
public class MatrixVolumeCompare implements AnalyzerTask<Pair<NumericMatrix, NumericMatrix>> {

    private static final Logger logger = Logger.getLogger(MatrixVolumeCompare.class);

    private final String dimension;

    private FileIOContext ioContext;

    private HistogramWriter histogramWriter;

    public MatrixVolumeCompare(String dimension) {
        this.dimension = dimension;
    }

    public void setIoContext(FileIOContext ioContext) {
        this.ioContext = ioContext;
    }

    public void setHistogramWriter(HistogramWriter histogramWriter) {
        this.histogramWriter = histogramWriter;
    }

    @Override
    public void analyze(Pair<NumericMatrix, NumericMatrix> matrices, List<StatsContainer> containers) {
        NumericMatrix refMatrix = matrices.getLeft();
        NumericMatrix simMatrix = matrices.getRight();

        NumericMatrix errMatrix = new NumericMatrix();
        MatrixOperations.errorMatrix(refMatrix, simMatrix, errMatrix);

        double[] errors = org.matsim.contrib.common.collections.CollectionUtils.toNativeArray(errMatrix.values());
        logger.debug(String.format("Compared %s od relations.", errors.length));

        String name = String.format("%s.err", dimension);
        StatsContainer container = new StatsContainer(name, errors);
        containers.add(container);

        if (histogramWriter != null)
            histogramWriter.writeHistograms(errors, name);

        if (ioContext != null) {
            try {
                /*
                write scatter plot
                */
                Set<String> keys = refMatrix.keys();
                keys.addAll(simMatrix.keys());

                TDoubleArrayList refVals = new TDoubleArrayList();
                TDoubleArrayList simVals = new TDoubleArrayList();
                for (String i : keys) {
                    for (String j : keys) {
                        Double refVol = refMatrix.get(i, j);
                        Double simVol = simMatrix.get(i, j);

                        if (refVol != null || simVol != null) {
                            if (refVol == null) refVol = 0.0;
                            if (simVol == null) simVol = 0.0;
                            refVals.add(refVol);
                            simVals.add(simVol);
                        }
                    }
                }

                StatsWriter.writeScatterPlot(refVals, simVals, dimension, "simulation", String.format
                        ("%s/%s.scatter.txt", ioContext.getPath(), dimension));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
