/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.stats;

import org.apache.log4j.Logger;

/**
 * Collects average number of operators, routes, passengers and vehicles and its variance in a recursive manner
 *  
 * @author aneumann
 *
 */
final class RecursiveStatsContainer {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(RecursiveStatsContainer.class);
	static final String toStringHeader = "# mean Operators; std dev Operators; mean Routes; std dev Routes; mean Pax; std dev Pax; mean Veh, std dev Veh"; 

	private double numberOfEntries = Double.NaN;
	
	private double arithmeticMeanOperators;
	private double tempVarOperators;
	private double arithmeticMeanRoutes;
	private double tempVarRoutes;
	private double arithmeticMeanPax;
	private double tempVarPax;
	private double arithmeticMeanVeh;
	private double tempVarVeh;
	
	public void handleNewEntry(double operators, double routes, double pax, double veh){
		// new entries n + 1
		double meanOperators_n_1;
		double tempVarOperators_n_1;
		double meanRoutes_n_1;
		double tempVarRoutes_n_1;
		double meanPax_n_1;
		double tempVarPax_n_1;
		double meanVeh_n_1;
		double tempVarVeh_n_1;

		if(Double.isNaN(this.numberOfEntries)){
			// initialize
			this.numberOfEntries = 0;
			this.arithmeticMeanOperators = 0;
			this.tempVarOperators = 0;
			this.arithmeticMeanRoutes = 0;
			this.tempVarRoutes = 0;
			this.arithmeticMeanPax = 0;
			this.tempVarPax = 0;
            this.arithmeticMeanVeh = 0;
			this.tempVarVeh = 0;
        }

		// calculate the exact mean and variance

		// calculate new mean
		meanOperators_n_1 =  (this.numberOfEntries * this.arithmeticMeanOperators + operators) / (this.numberOfEntries + 1);
		meanRoutes_n_1 =  (this.numberOfEntries * this.arithmeticMeanRoutes + routes) / (this.numberOfEntries + 1);
		meanPax_n_1 =  (this.numberOfEntries * this.arithmeticMeanPax + pax) / (this.numberOfEntries + 1);
		meanVeh_n_1 =  (this.numberOfEntries * this.arithmeticMeanVeh + veh) / (this.numberOfEntries + 1);

		if (this.numberOfEntries == 0) {
			tempVarOperators_n_1 = 0;
			tempVarRoutes_n_1 = 0;
			tempVarPax_n_1 = 0;
			tempVarVeh_n_1 = 0;
		} else {
			tempVarOperators_n_1 = this.tempVarOperators + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanOperators_n_1 - operators) * (meanOperators_n_1 - operators);
			tempVarRoutes_n_1 = this.tempVarRoutes + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanRoutes_n_1 - routes) * (meanRoutes_n_1 - routes);
			tempVarPax_n_1 = this.tempVarPax + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanPax_n_1 - pax) * (meanPax_n_1 - pax);
			tempVarVeh_n_1 = this.tempVarVeh + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanVeh_n_1 - veh) * (meanVeh_n_1 - veh);
		}
		
		this.numberOfEntries++;

		// store em away
		this.arithmeticMeanOperators = meanOperators_n_1;
		this.tempVarOperators = tempVarOperators_n_1;
		this.arithmeticMeanRoutes = meanRoutes_n_1;
		this.tempVarRoutes = tempVarRoutes_n_1;
		this.arithmeticMeanPax = meanPax_n_1;
		this.tempVarPax = tempVarPax_n_1;
		this.arithmeticMeanVeh = meanVeh_n_1;
		this.tempVarVeh = tempVarVeh_n_1;
	}

	public double getArithmeticMeanOperators() {
		return this.arithmeticMeanOperators;
	}

	public double getStdDevOperators() {

		if (this.numberOfEntries > 1){
			return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarOperators);
		}
		
		return Double.NaN;
	}
	
	public double getArithmeticMeanRoutes() {
		return this.arithmeticMeanRoutes;
	}

	public double getStdDevRoutes() {

		if (this.numberOfEntries > 1){
			return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarRoutes);
		}
		
		return Double.NaN;
	}

	public double getArithmeticMeanPax() {
		return this.arithmeticMeanPax;
	}

	public double getStdDevPax() {

		if (this.numberOfEntries > 1){
			return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarPax);
		}
		
		return Double.NaN;
	}

	public double getArithmeticMeanVeh() {
		return this.arithmeticMeanVeh;
	}

	public double getStdDevVeh() {

		if (this.numberOfEntries > 1){
			return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarVeh);
		}
		
		return Double.NaN;
	}

	@Override
	public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this.getArithmeticMeanOperators()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevOperators()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanRoutes()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevRoutes()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanPax()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevPax()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanVeh()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevVeh()); strBuffer.append("; ");
		return strBuffer.toString();
	}	
}