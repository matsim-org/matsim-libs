/* *********************************************************************** *
 * project: org.matsim.*
 * MySaFitnessFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.RetailerSA;

import java.util.ArrayList;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class MySaFitnessFunction {
	
	private DenseDoubleMatrix2D p;
	private DenseDoubleMatrix2D d;
	private DenseDoubleMatrix1D D;
	
	public MySaFitnessFunction(DenseDoubleMatrix2D observedLikelyhood, DenseDoubleMatrix2D distance, DenseDoubleMatrix1D retailerSize){
		this.p = observedLikelyhood;
		this.d = distance;
		this.D = retailerSize;
	}
	
	public double evaluate(ArrayList<Double> thisSolution){
		double betaOne = thisSolution.get(0);
		double betaTwo = thisSolution.get(1);
		Double result = 0.0;
		
		double thisCalculatedP;
		for(int row = 0; row < p.rows(); row++) {
			//TODO Calculate the denominator
			double denominator = 0.0;
			for(int column = 0; column < p.columns(); column++){
				denominator += Math.pow(d.get(row, column), betaOne)*Math.pow(D.get(column), betaTwo);
			}
			
			for(int column = 0; column < p.columns(); column++){
				thisCalculatedP = (Math.pow(d.get(row, column), betaOne)) * (Math.pow(D.get(column), betaTwo)) / denominator;
				result += Math.pow((thisCalculatedP - p.get(row, column)), 2);
			}
		}
		return result;
	}
}
