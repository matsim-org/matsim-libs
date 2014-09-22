/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.andreas.P2.genericUtils;

/**
 * Provides arithmetic mean and standard deviation.
 * 
 * @author aneumann
 *
 */
public final class RecursiveStatsContainer {

	private double numberOfEntries = Double.NaN;
	private double arithmeticMean;
	private double tempVar;

	public void handleNewEntry(double entry){
		// new entries n + 1
		double meanOperators_n_1;
		double tempVarOperators_n_1;

		if(Double.isNaN(this.numberOfEntries)){
			// initialize
			this.numberOfEntries = 0;
			this.arithmeticMean = 0;
			this.tempVar = 0;
		}

		// calculate the exact mean and variance

		// calculate new mean
		meanOperators_n_1 =  (this.numberOfEntries * this.arithmeticMean + entry) / (this.numberOfEntries + 1);

		if (this.numberOfEntries == 0) {
			tempVarOperators_n_1 = 0;
		} else {
			tempVarOperators_n_1 = this.tempVar + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanOperators_n_1 - entry) * (meanOperators_n_1 - entry);
		}

		this.numberOfEntries++;

		// store em away
		this.arithmeticMean = meanOperators_n_1;
		this.tempVar = tempVarOperators_n_1;
	}

	public double getStdDev() {
		if (this.numberOfEntries > 1){
			return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVar);
		}			
		return Double.NaN;
	}

	public double getMean() {
		return this.arithmeticMean;
	}
	
	public int getNumberOfEntries(){
		return (int) this.numberOfEntries;
	}
}
