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

public class MyWalkDistanceConverter {
	private final Logger log = Logger.getLogger(MyWalkDistanceConverter.class);
	private String studyArea;
	private int studyAreaCode;
	
	/**
	 * Estimates the walk time to public transport (in seconds) as function of
	 * the distance from the zonal centroid to the closest public transport
	 * node.
	 * @param studyArea
	 */
	public MyWalkDistanceConverter(String studyArea) {
		this.studyArea = studyArea;
		if(this.studyArea.equalsIgnoreCase("eThekwini")){
			this.studyAreaCode = 1;
		} else{
			log.warn("Do not have a distance -> time conversion function for " + studyArea);
			throw new RuntimeException("Unable to convert.");
		}
	}

	/**
	 * Converts a given distance value (in meters) into a time value (in sec).
	 * @param distance , expressed in meters.
	 * @return time, expressed in seconds.
	 */
	public Double convert(Double distance){
		Double time = null;
		switch (studyAreaCode) {
		case 1: // eThekwini
			time = 5.859e-01 + 1.070e-02*distance -1.464e-06*Math.pow(distance,2) +2.695*Math.pow(distance, 3);
			break;
		default:
			break;
		}
		return time;
	}

}