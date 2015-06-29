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

import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SpatialAveragingParameters {

	private int numberOfXBins = 160;
	private int numberOfYBins = 120;
	private double smoothingRadius_m = 1000.;
	private double smoothingRadiusSquared_m;
	private double areaInSmoothingCirleSqkm;
	private String visBoundaryShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
	private CoordinateReferenceSystem targetCRS = null;
	private boolean isUsingVisBoundary = false;

	public SpatialAveragingParameters(){
		smoothingRadiusSquared_m = smoothingRadius_m*smoothingRadius_m;
		areaInSmoothingCirleSqkm = Math.PI * smoothingRadiusSquared_m /1000. /1000.;
	}
	public int getNoOfXbins() {
		return numberOfXBins;
	}

	public int getNoOfYbins() {
		return numberOfYBins;
	}

	public double getSmoothingRadiusSquared_m() {
		return smoothingRadiusSquared_m;
	}

	public double getAreaInSmoothingCirleSqKM() {
		return areaInSmoothingCirleSqkm;
	}

//	public String getVisBoundaryShapeFile() {
//		return visBoundaryShapeFile;
//	}

	public CoordinateReferenceSystem getTargetCRS() {
		return targetCRS;
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
