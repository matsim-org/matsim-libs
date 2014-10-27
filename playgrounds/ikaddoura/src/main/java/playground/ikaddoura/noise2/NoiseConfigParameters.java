/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.ikaddoura.noise2;

/**
 * @author lkroeger
 *
 */

public class NoiseConfigParameters {
	
	public static double getIntervalLength(){
		// 7200. should be a multiple of the interval length,
		// otherwise the calculation of the day and night values is not correct
		double intervalLength = 3600.0;
		return intervalLength;
	}
	
	public static double getTimeBinSize(){
		double timeBinSize = 3600.0;
		return timeBinSize;
	}
	
	public static double getScaleFactor(){
		double scaleFactor = 1.;
		return scaleFactor;
	}
}
