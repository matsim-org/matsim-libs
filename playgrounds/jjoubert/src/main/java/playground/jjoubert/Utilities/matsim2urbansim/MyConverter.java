/* *********************************************************************** *
 * project: org.matsim.*
 * MyWalkTimeEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

import org.apache.log4j.Logger;

public class MyConverter {
	private final Logger log = Logger.getLogger(MyConverter.class);
	private String studyArea;
	private int studyAreaCode;
	
	/**
	 * Estimates a number of conversion factors. The conversions are required to
	 * infer travel data for UrbanSim from MATSim results:
	 * <ul>
	 * 		<li> the walk time to public transport (in seconds) as a function 
	 * 			 of the distance from the zonal centroid to the closest public 
	 * 			 transport node.
	 * 		<li> the public transport travel time as a function of private car
	 * 			 travel time.
	 * </ul>
	 * @param studyArea
	 */
	public MyConverter(String studyArea) {
		this.studyArea = studyArea;
		if(this.studyArea.equalsIgnoreCase("eThekwini")){
			this.studyAreaCode = 1;
		} else{
			log.warn("Do not have a distance -> time conversion function for " + studyArea);
			throw new RuntimeException("Unable to convert.");
		}
	}

	/**
	 * Converts a given walking distance value (in meters) into a walking time 
	 * value (in sec).
	 * @param walkDistance , expressed in meters.
	 * @return walkTime, expressed in seconds.
	 */
	public Double convertWalkDistanceToWalkTime(double walkDistance){
		Double walkTime = null;
		switch (studyAreaCode) {
		case 1: // eThekwini
			double a = 5.858772e-01;
			double b = 1.070014e-02;
			double c = -1.464301e-06;
			double d = 2.694900e-09;
			double wt = a + 
						b*walkDistance + 
						c*Math.pow(walkDistance,2) + 
						d*Math.pow(walkDistance, 3);
			/*
			 * Since the model fitting in R was done on walking time in MINUTES,
			 * we need to convert it back to seconds, just to be consistent.
			 */
			walkTime = wt*60;
			break;
		default:
			break;
		}
		return walkTime;
	}
	
	/**
	 * Converts a given private car travel time (in seconds) into a public
	 * transport travel time (in seconds).
	 * @param carTime , private car travel time expressed in seconds.
	 * @return ptTime, estimated public transport travel time expressed in minutes. 
	 */
	public Double convertCarTimeToPtTime(double carTime){
		Double ptTime = null;
		/*
		 * Model fitting was done in R using minutes as the unit of measure.
		 */
		double ct = carTime / 60;
		switch (studyAreaCode) {
		case 1: // eThekwini
			/* The following parameters were obtained after fitting a 5th
			 * degree polynomial to the logs of public transport travel time 
			 * (from NHTS) against private car travel time (from MATSim). 
			 * Last update: 20 July 2010 (jwjoubert)
			 */
			double a =	2.43874904018E+000;
			double b =	3.78853675201E-001;
			double c =	1.52594038400E-002;
			double d =	2.04099913494E-003;
			double e =	1.09340128891E-004;
			double f =	2.76219195512E-004;
			double pt = Math.exp(
					a + 
					b*Math.log(ct) + 
					c*Math.pow(Math.log(ct), 2) + 
					d*Math.pow(Math.log(ct), 3) +
					e*Math.pow(Math.log(ct), 4) + 
					f*Math.pow(Math.log(ct), 5));
			ptTime = pt*60;
			break;

		default:
			break;
		}
		return ptTime;
	}
	
	

}