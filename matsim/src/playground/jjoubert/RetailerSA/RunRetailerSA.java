/* *********************************************************************** *
 * project: org.matsim.*
 * RunRetailerSA.java
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

import org.apache.log4j.Logger;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class RunRetailerSA {
	
	private final static Logger log = Logger.getLogger(RunRetailerSA.class);

	
	public static void main(String[] args){
		log.info("Initiating the Simulated Annealing (SA) parameter estimation model.");
		
		// TODO Read in the existing p, d and D values.
		DenseDoubleMatrix2D p  = null;
		DenseDoubleMatrix2D d = null;
		DenseDoubleMatrix1D D = null;
		
		MySaFitnessFunction ff = new MySaFitnessFunction(p, d, D);
		
		
		
		
	}

}
