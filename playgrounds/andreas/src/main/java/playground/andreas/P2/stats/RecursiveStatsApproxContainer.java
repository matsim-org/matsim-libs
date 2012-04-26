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

package playground.andreas.P2.stats;

import org.apache.log4j.Logger;

/**
 * Collects average number of cooperatives, passengers and vehicles and its variance in a recursive manner using exponential smoothing
 *  
 * @author aneumann
 *
 */
public class RecursiveStatsApproxContainer {
	
	private static final Logger log = Logger.getLogger(RecursiveStatsApproxContainer.class);
	static final String toStringHeader = "# ES mean Coops; ES std dev Coops; ES mean Pax; ES std dev Pax; ES mean Veh, ES std dev Veh"; 

	private final double alpha;
	private final int initNEntries;
	
	private double numberOfEntries = Double.NaN;
	
	private double arithmeticMeanCoops;
	private double tempVarCoops;
	private double stdDevCoops;
	
	private double arithmeticMeanPax;
	private double tempVarPax;
	private double stdDevPax;
	
	private double arithmeticMeanVeh;
	private double tempVarVeh;
	private double stdDevVeh;
	
	public RecursiveStatsApproxContainer(double alpha, int initNEntries) {
		this.alpha = alpha;
		this.initNEntries = initNEntries;
	}
	
	public void handleNewEntry(double coops, double pax, double veh){
		
		if(Double.isNaN(this.numberOfEntries)){
			// initialize
			this.numberOfEntries = 0;
			this.arithmeticMeanCoops = 0;
			this.tempVarCoops = 0;
			this.stdDevCoops = 0;
			this.arithmeticMeanPax = 0;
			this.tempVarPax = 0;
			this.stdDevPax = 0;
			this.arithmeticMeanVeh = 0;
			this.tempVarVeh = 0;
			this.stdDevVeh = 0;
		}		

		if(this.numberOfEntries < initNEntries){
			// new entries n + 1
			double meanCoops_n_1;
			double tempVarCoops_n_1;
			double meanPax_n_1;
			double tempVarPax_n_1;
			double meanVeh_n_1;
			double tempVarVeh_n_1;
			
			// calculate the exact mean and variance
			meanCoops_n_1 =  (this.numberOfEntries * this.arithmeticMeanCoops + coops) / (this.numberOfEntries + 1);
			meanPax_n_1 =  (this.numberOfEntries * this.arithmeticMeanPax + pax) / (this.numberOfEntries + 1);
			meanVeh_n_1 =  (this.numberOfEntries * this.arithmeticMeanVeh + veh) / (this.numberOfEntries + 1);

			if (this.numberOfEntries == 0) {
				tempVarCoops_n_1 = 0;
				tempVarPax_n_1 = 0;
				tempVarVeh_n_1 = 0;
			} else {
				tempVarCoops_n_1 = this.tempVarCoops + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanCoops_n_1 - coops) * (meanCoops_n_1 - coops);
				tempVarPax_n_1 = this.tempVarPax + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanPax_n_1 - pax) * (meanPax_n_1 - pax);
				tempVarVeh_n_1 = this.tempVarVeh + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanVeh_n_1 - veh) * (meanVeh_n_1 - veh);
			}

			this.numberOfEntries++;
			
			// store em away
			this.arithmeticMeanCoops = meanCoops_n_1;
			this.tempVarCoops = tempVarCoops_n_1;
			this.arithmeticMeanPax = meanPax_n_1;
			this.tempVarPax = tempVarPax_n_1;
			this.arithmeticMeanVeh = meanVeh_n_1;
			this.tempVarVeh = tempVarVeh_n_1;
			
			this.stdDevCoops = this.getStdDevCoop();
			this.stdDevPax = this.getStdDevPax();
			this.stdDevVeh = this.getStdDevVeh();
			
		} else {			
			// new entries n + 1
			double meanCoops_n_1;
			double tempStdDevCoops_n_1;
			double meanPax_n_1;
			double tempStdDevPax_n_1;
			double meanVeh_n_1;
			double tempStdDevVeh_n_1;
			
			// approximate the mean and variance			
			meanCoops_n_1 = (1 - this.alpha) * this.arithmeticMeanCoops + this.alpha * coops;
			meanPax_n_1 = (1 -  this.alpha) * this.arithmeticMeanPax + this.alpha * pax;
			meanVeh_n_1 = (1 - this.alpha) * this.arithmeticMeanVeh + this.alpha * veh;
			
			tempStdDevCoops_n_1 = (1 - this.alpha) * this.stdDevCoops + this.alpha * Math.sqrt((meanCoops_n_1 - coops) * (meanCoops_n_1 - coops));
			tempStdDevPax_n_1 = (1 - this.alpha) * this.stdDevPax + this.alpha * Math.sqrt((meanPax_n_1 - pax) * (meanPax_n_1 - pax));
			tempStdDevVeh_n_1 = (1 - this.alpha) * this.stdDevVeh + this.alpha * Math.sqrt((meanVeh_n_1 - veh) * (meanVeh_n_1 - veh));
			
			this.numberOfEntries++;
			
			// store em away
			this.arithmeticMeanCoops = meanCoops_n_1;
			this.stdDevCoops = tempStdDevCoops_n_1;
			this.arithmeticMeanPax = meanPax_n_1;
			this.stdDevPax = tempStdDevPax_n_1;
			this.arithmeticMeanVeh = meanVeh_n_1;
			this.stdDevVeh = tempStdDevVeh_n_1;
		}
	}

	public double getArithmeticMeanCoops() {
		return this.arithmeticMeanCoops;
	}

	public double getStdDevCoop() {

		if (this.numberOfEntries > 1){
			if (this.numberOfEntries <= this.initNEntries) {
				return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarCoops);				
			} else {
				return this.stdDevCoops;
			}			
		}
		
		return Double.NaN;
	}

	public double getArithmeticMeanPax() {
		return this.arithmeticMeanPax;
	}

	public double getStdDevPax() {

		if (this.numberOfEntries > 1){
			if (this.numberOfEntries <= this.initNEntries) {
				return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarPax);				
			} else {
				return this.stdDevPax;
			}
		}
		
		return Double.NaN;
	}

	public double getArithmeticMeanVeh() {
		return this.arithmeticMeanVeh;
	}

	public double getStdDevVeh() {

		if (this.numberOfEntries > 1){
			if (this.numberOfEntries <= this.initNEntries) {
				return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarVeh);				
			} else {
				return this.stdDevVeh;
			}
		}
		
		return Double.NaN;
	}

	@Override
	public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this.getArithmeticMeanCoops()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevCoop()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanPax()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevPax()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanVeh()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevVeh()); strBuffer.append("; ");
		return strBuffer.toString();
	}	
}