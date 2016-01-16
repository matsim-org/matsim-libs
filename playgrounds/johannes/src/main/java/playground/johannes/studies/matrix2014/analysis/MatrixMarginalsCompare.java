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

import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.util.List;

/**
 * @author johannes
 */
public class MatrixMarginalsCompare implements AnalyzerTask<NumericMatrix> {

    private NumericMatrix refMatrix;

    public void setReferenceMatrix(NumericMatrix refMatrix) {
        this.refMatrix = refMatrix;
    }

    @Override
    public void analyze(NumericMatrix simMatrix, List<StatsContainer> containers) {
        System.out.println(String.format("Diagonal sim matrix: %s", MatrixOperations.diagonalSum(simMatrix)));
        System.out.println(String.format("Diagonal ref matrix: %s", MatrixOperations.diagonalSum(refMatrix)));
    }
}
