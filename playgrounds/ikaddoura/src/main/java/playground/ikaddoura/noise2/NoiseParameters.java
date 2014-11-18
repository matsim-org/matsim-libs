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

import org.apache.log4j.Logger;

/**
 * @author ikaddoura
 *
 */
public class NoiseParameters {
	private static final Logger log = Logger.getLogger(NoiseParameters.class);

	// default values
	private double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));
	private double timeBinSizeNoiseComputation = 3600.0;
	private double timeBinSizeRouter = 3600.0;
	private double scaleFactor = 1.;
	private double receiverPointGap = 250.;
	private double relevantRadius = 500.;
	private String hgvIdPrefix = "lkw";
	
	public void setAnnualCostRate(double annualCostRate) {
		log.info("Setting the annual cost rate to " + annualCostRate);
		this.annualCostRate = annualCostRate;
	}

	public void setTimeBinSizeNoiseComputation(double timeBinSizeNoiseComputation) {
		log.info("Setting the time bin size for the computation of noise to " + timeBinSizeNoiseComputation);
		this.timeBinSizeNoiseComputation = timeBinSizeNoiseComputation;
	}

	public void setTimeBinSizeRouter(double timeBinSizeRouter) {
		log.info("Setting the time bin size for the router to " + timeBinSizeRouter);
		this.timeBinSizeRouter = timeBinSizeRouter;
	}

	public void setScaleFactor(double scaleFactor) {
		log.info("Setting the scale factor to " + scaleFactor);
		this.scaleFactor = scaleFactor;
	}

	public void setReceiverPointGap(double receiverPointGap) {
		log.info("Setting the horizontal/vertical distance between each receiver point to " + receiverPointGap);
		this.receiverPointGap = receiverPointGap;
	}

	public void setRelevantRadius(double relevantRadius) {
		log.info("Setting the radius of relevant links around each receiver point to " + relevantRadius);
		this.relevantRadius = relevantRadius;
	}
	
	public void setHgvIdPrefix(String hgvIdPrefix) {
		log.info("Setting the HGV Id Prefix to " + hgvIdPrefix);
		this.hgvIdPrefix = hgvIdPrefix;
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

	public String getHgvIdPrefix() {
		return hgvIdPrefix;
	}

}
