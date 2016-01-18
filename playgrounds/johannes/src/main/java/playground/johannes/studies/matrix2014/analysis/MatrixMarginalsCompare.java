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
import gnu.trove.map.TObjectDoubleMap;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.HistogramWriter;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 */
public class MatrixMarginalsCompare implements AnalyzerTask<NumericMatrix> {

    private NumericMatrix refMatrix;

    private String refMatrixName;

    private HistogramWriter writer;

    public void setReferenceMatrix(NumericMatrix refMatrix, String refMatrixName) {
        this.refMatrix = refMatrix;
        this.refMatrixName = refMatrixName;
    }

    public void setHistogramWriter(HistogramWriter writer) {
        this.writer = writer;
    }

    @Override
    public void analyze(NumericMatrix simMatrix, List<StatsContainer> containers) {
        /*
        diagonal errors
         */
        TDoubleArrayList errors = new TDoubleArrayList();

        Set<String> keys = refMatrix.keys();
        keys.addAll(simMatrix.keys());
        for(String key : keys) {
            Double simVol = simMatrix.get(key, key);
            Double refVol = refMatrix.get(key, key);
            Double error = relativeError(simVol, refVol);
            if(error != null) errors.add(error);
        }

        String dimension = String.format("matrix.diagonal.err.%s", refMatrixName);
        double[] nativeErrors = errors.toArray();
        StatsContainer container = new StatsContainer(dimension, nativeErrors);
        containers.add(container);

        if(writer != null) {
            writer.writeHistograms(nativeErrors, dimension);
        }
        /*
        diagonal sum error
         */
        double simDia = MatrixOperations.diagonalSum(simMatrix);
        double refDia = MatrixOperations.diagonalSum(refMatrix);
        container = new StatsContainer(String.format("matrix.diagonal.sum.err.%s", refMatrixName));
        container.setMean((simDia - refDia)/refDia);
        containers.add(container);
        /*
        row marginals
         */
        TObjectDoubleMap<String> simRowSums = MatrixOperations.rowMarginals(simMatrix);
        TObjectDoubleMap<String> refRowSums = MatrixOperations.rowMarginals(refMatrix);
        analyzeMarginals(simRowSums, refRowSums, containers, "row");

        simRowSums = MatrixOperations.columnMarginals(simMatrix);
        refRowSums = MatrixOperations.columnMarginals(refMatrix);
        analyzeMarginals(simRowSums, refRowSums, containers, "col");
    }

    private void analyzeMarginals(TObjectDoubleMap<String> simRowSums, TObjectDoubleMap<String> refRowSums, List<StatsContainer> containers, String name) {
        Set<String> keys = new HashSet<>();
        for(Object key : simRowSums.keys()) keys.add((String)key);
        for(Object key : refRowSums.keys()) keys.add((String)key);

        double simRowSum = 0;
        double refRowSum = 0;
        TDoubleArrayList errors = new TDoubleArrayList();
        for(String key : keys) {
            Double simVol = simRowSums.get(key);
            Double refVol = refRowSums.get(key);
            Double error = relativeError(simVol, refVol);
            if(error != null) errors.add(error);

            if(simVol != null) simRowSum += simVol;
            if(refVol != null) refRowSum += refVol;
        }

        String dimension = String.format("matrix.%s.err.%s", name, refMatrixName);
        double[] nativeErrors = errors.toArray();
        containers.add(new StatsContainer(dimension, nativeErrors));

        if(writer != null) writer.writeHistograms(nativeErrors, dimension);

        dimension = String.format("matrix.%s.sum.err.%s", name, refMatrixName);
        StatsContainer container = new StatsContainer(dimension);
        container.setMean((simRowSum - refRowSum)/refRowSum);
        containers.add(container);
    }

    private Double relativeError(Double simVol, Double refVol) {
        if(refVol != null || simVol != null) {
            if (simVol == null) simVol = 0.0;
            if (refVol == null) refVol = 0.0;

            if (refVol == 0.0 && simVol == 0.0) {
                return new Double(0);
            } else if(refVol > 0.0) {
                return (simVol - refVol) / refVol;
            }
        }

        return null;
    }
}
