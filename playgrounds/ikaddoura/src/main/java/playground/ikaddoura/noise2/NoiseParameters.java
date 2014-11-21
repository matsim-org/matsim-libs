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

//	private String[] tunnelLinks = null;
	
	// for Berlin
	private String[] tunnelLinks = {
			"108041",
			"108142",
			"108970",
			"109085",
			"109757",
			"109919",
			"110060",
			"110226",
			"110164",
			"110399",
			"96503",
			"110389",
			"110116",
			"110355",
			"92604",
			"92603",
			"25651",
			"25654",
			"112540",
			"112556",
			"5052",
			"5053",
			"5380",
			"5381",
			"106309",
			"106308",
			"26103",
			"26102",
			"4376",
			"4377",
			"106353",
			"106352",
			"103793",
			"103792",
			"26106",
			"26107",
			"4580",
			"4581",
			"4988",
			"4989",
			"73496",
			"73497"
	};
	
	// for the entire area with activity locations
	private double xMin = 0.;
	private double yMin = 0.;
	private double xMax = 0.;
	private double yMax = 0.;
	
	// for the area around the city center of Berlin (Tiergarten)
//	private double xMin = 4590855.;
//	private double yMin = 5819679.;
//	private double xMax = 4594202.;
//	private double yMax = 5821736.;
	
//	 for the area of Berlin
//	private double xMin = 4573258.;
//	private double yMin = 5801225.;
//	private double xMax = 4620323.;
//	private double yMax = 5839639.;
		
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

	public double getxMin() {
		return xMin;
	}

	public void setxMin(double xMin) {
		this.xMin = xMin;
	}

	public double getyMin() {
		return yMin;
	}

	public void setyMin(double yMin) {
		this.yMin = yMin;
	}

	public double getxMax() {
		return xMax;
	}

	public void setxMax(double xMax) {
		this.xMax = xMax;
	}

	public double getyMax() {
		return yMax;
	}

	public void setyMax(double yMax) {
		this.yMax = yMax;
	}

	public String[] getTunnelLinks() {
		return tunnelLinks;
	}

	public void setTunnelLinks(String[] tunnelLinks) {
		this.tunnelLinks = tunnelLinks;
	}

}
