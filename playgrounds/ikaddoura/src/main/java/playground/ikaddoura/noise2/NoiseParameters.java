/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.noise2;

/**
 * @author ikaddoura
 *
 */
public class NoiseParameters {
	
	// default values
	private double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));
	private double timeBinSizeNoiseComputation = 3600.0;
	private double timeBinSizeRouter = 3600.0;
	private double scaleFactor = 1.;
	private double receiverPointGap = 250.;
	private double relevantRadius = 500.;
	
	public void setAnnualCostRate(double annualCostRate) {
		this.annualCostRate = annualCostRate;
	}

	public void setTimeBinSizeNoiseComputation(double timeBinSizeNoiseComputation) {
		this.timeBinSizeNoiseComputation = timeBinSizeNoiseComputation;
	}

	public void setTimeBinSizeRouter(double timeBinSizeRouter) {
		this.timeBinSizeRouter = timeBinSizeRouter;
	}

	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public void setReceiverPointGap(double receiverPointGap) {
		this.receiverPointGap = receiverPointGap;
	}

	public void setRelevantRadius(double relevantRadius) {
		this.relevantRadius = relevantRadius;
	}

	public double getAnnualCostRate() {
		return annualCostRate;
	}
	
	public double getTimeBinSizeNoiseComputation() {
		return timeBinSizeNoiseComputation;
	}
	
	public double getTimeBinSizeRouter() {
		return timeBinSizeRouter;
	}
	
	public double getScaleFactor() {
		return scaleFactor;
	}
	
	public double getReceiverPointGap() {
		return receiverPointGap;
	}
	
	public double getRelevantRadius() {
		return relevantRadius;
	}

}
