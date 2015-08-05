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

package playground.benjamin.scenarios.munich.analysis.spatialAvg;


public class SpatialAveragingParameters {

	private int numberOfXBins = 160;
	private int numberOfYBins = 120;
	private double smoothingRadius_m = 32.;
	private boolean isUsingVisBoundary = false;

	public int getNoOfXbins() {
		return numberOfXBins;
	}

	public int getNoOfYbins() {
		return numberOfYBins;
	}

	public Double getSmoothingRadius_m() {
		return this.smoothingRadius_m;
	}
	
	public boolean IsUsingVisBoundary() {
		return isUsingVisBoundary ;
	}
	
	public Double getNoOfBins() {
		return new Double(numberOfXBins*numberOfYBins);
	}
}
