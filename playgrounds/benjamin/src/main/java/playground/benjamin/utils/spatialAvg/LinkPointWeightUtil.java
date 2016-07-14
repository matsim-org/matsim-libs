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

package playground.benjamin.utils.spatialAvg;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


public class LinkPointWeightUtil implements LinkWeightUtil {

	private static final Logger logger = Logger.getLogger(LinkPointWeightUtil.class);
	
	double xMin;
	double xMax;
	double yMin;
	double yMax;
	int noOfXbins;
	int noOfYbins;
	
	double smoothinRadiusSquared_m;
	double area_in_smoothing_circle_sqkm;
	CoordinateReferenceSystem targetCRS;

	private double cellSizeSquareKm;
	
	public LinkPointWeightUtil(double xMin, double xMax, double yMin, double yMax, int noOfXbins, int noOfYbins, double smoothingRadius_m, CoordinateReferenceSystem targetCRS) {
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
		this.noOfXbins = noOfXbins;
		this.noOfYbins = noOfYbins;
		
		this.smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
		this.area_in_smoothing_circle_sqkm = (Math.PI * smoothingRadius_m * smoothingRadius_m) / (1000. * 1000.);
		this.targetCRS = targetCRS;
		this.cellSizeSquareKm = (xMax-xMin)/noOfXbins*(yMax-yMin)/noOfYbins / (1000.*1000.);
		logger.info("Cell size in sqkm is " + this.cellSizeSquareKm);
	}
	
	public LinkPointWeightUtil(SpatialAveragingInputData inputData, int noOfXbins2, int noOfYbins2, double smoothingRadius_m) {
		this(inputData.getMinX(),
			 inputData.getMaxX(), 
			 inputData.getMinY(),
			 inputData.getMaxY(), 
			 noOfXbins2,
			 noOfYbins2, 
			 smoothingRadius_m, 
			 inputData.getTargetCRS());
	}

	public LinkPointWeightUtil(SpatialAveragingInputData inputData) {
		this(inputData, inputData.getNoOfXbins(), inputData.getNoOfYbins(), inputData.getSmoothingRadius_m());
	}

	@Override
	public Double getWeightFromLink(Link link, Coord cellCentroid) {
		double linkcenterx = link.getCoord().getX();
		double linkcentery = link.getCoord().getY();
		double cellCentroidX = cellCentroid.getX();
		double cellCentroidY = cellCentroid.getY();
		return calculateWeightOfPointForCell(linkcenterx, linkcentery, cellCentroidX, cellCentroidY);
	}

	@Override
	public Double getNormalizationFactor() {
		return (cellSizeSquareKm/this.area_in_smoothing_circle_sqkm);
	}
	
	private double calculateWeightOfPointForCell(double x1, double y1, double x2, double y2) {
		double distanceSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
		return Math.exp((-distanceSquared) / (smoothinRadiusSquared_m));
	}

	@Override
	public Double getWeightFromCoord(Coord emittingCoord, Coord receivingCoord) {
		return calculateWeightOfPointForCell(emittingCoord.getX(), emittingCoord.getY(), receivingCoord.getX(), receivingCoord.getY());
	}
}