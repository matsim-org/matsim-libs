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

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.util.List;

/**
 * @author johannes
 */
public class MatrixIntraVolumeShare implements AnalyzerTask<NumericMatrix> {

    private TObjectDoubleMap<String> rowShare;

    private TObjectDoubleMap<String> colShare;

    public TObjectDoubleMap<String> getRowShare() {
        return rowShare;
    }

    public TObjectDoubleMap<String> getColShare() {
        return colShare;
    }

    @Override
    public void analyze(NumericMatrix matrix, List<StatsContainer> containers) {
        TObjectDoubleMap<String> rowMarginals = MatrixOperations.rowMarginals(matrix);
        rowShare = fractions(rowMarginals, matrix);

        TObjectDoubleMap<String> colMarginals = MatrixOperations.columnMarginals(matrix);
        colShare = fractions(colMarginals, matrix);
    }

    private TObjectDoubleMap<String> fractions(TObjectDoubleMap<String> marginals, NumericMatrix matrix) {
        TObjectDoubleMap<String> fractions = new TObjectDoubleHashMap<>();
        TObjectDoubleIterator<String> it = marginals.iterator();
        for(int i = 0; i < marginals.size(); i++) {
            it.advance();
            String key = it.key();
            double sum = it.value();
            Double intra = matrix.get(key, key);
            if(intra == null) intra = 0.0;
            fractions.put(key, intra/sum);
        }

        return fractions;
    }
}
