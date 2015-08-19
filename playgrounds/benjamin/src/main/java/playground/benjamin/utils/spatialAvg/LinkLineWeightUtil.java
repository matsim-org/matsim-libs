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

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

public class LinkLineWeightUtil implements LinkWeightUtil {
	private final Logger logger = Logger.getLogger(LinkLineWeightUtil.class);
	private double smoothingRadiusSquared_m;
	private double smoothingRadius_m;
	private double cellsize_m;
	
	public LinkLineWeightUtil(double smoothingRadius_m, double cellSizeSquareMeter) {
		this.smoothingRadius_m = smoothingRadius_m;
		this.smoothingRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
		this.cellsize_m = cellSizeSquareMeter;
	}
	
	public LinkLineWeightUtil(SpatialAveragingInputData inputData) {
		this(inputData.getSmoothingRadius_m(), inputData.getBoundingboxSizeSquareMeter()/inputData.getNoOfBins());
	}
	
	@Override
	public Double getWeightFromLink(Link link, Coord cellCentroid) {
		Coord fromNodeCoord = link.getFromNode().getCoord();
		Coord toNodeCoord = link.getToNode().getCoord();
		double cellCentroidX = cellCentroid.getX();
		double cellCentroidY = cellCentroid.getY();
		double constantA = getConstantA(fromNodeCoord, cellCentroidX, cellCentroidY);
		double constantB = getConstantB(fromNodeCoord, toNodeCoord, cellCentroidX, cellCentroidY);
		double constantC =0.;
		double linkLengthFromCoord = getLinkLengthUsingFromAndToNode(fromNodeCoord, toNodeCoord);
		double constantR = Math.sqrt(smoothingRadiusSquared_m);
		constantC= (constantR*(Math.sqrt(Math.PI))/(linkLengthFromCoord*2))*Math.exp(-(constantA-(constantB*constantB/(linkLengthFromCoord*linkLengthFromCoord)))/smoothingRadiusSquared_m);

		double upperLimit = (linkLengthFromCoord+(constantB/linkLengthFromCoord));
		double lowerLimit = constantB/(linkLengthFromCoord);
		double integrationUpperLimit;
		double integrationLowerLimit;
		try {
			integrationUpperLimit = Erf.erf(upperLimit/constantR);
			integrationLowerLimit = Erf.erf(lowerLimit/constantR);
		} catch (MathException e) {
			throw new RuntimeException("Error function is not defined for " + upperLimit + " or " + lowerLimit + "; Exception: " + e);
		}
		double  weight = constantC *(integrationUpperLimit-integrationLowerLimit);
		if(weight<0.0) {
			throw new RuntimeException("Weight is negative: weight = " + weight + ". Aborting...");
		} else if (Double.isNaN(weight)) weight = 0.0;
		return weight;
	}
	private double getConstantA(Coord fromNodeCoord, double cellCentroidX, double cellCentroidY){
		double x1 = fromNodeCoord.getX();
		double y1= fromNodeCoord.getY();
		double x0 = cellCentroidX;
		double y0 = cellCentroidY;
		return (x1-x0)*(x1-x0)+(y1-y0)*(y1-y0);
	}

	private double getLinkLengthUsingFromAndToNode(Coord fromNodeCoord, Coord toNodeCoord){
		double x1 = fromNodeCoord.getX();
		double y1 = fromNodeCoord.getY();
		double x2 = toNodeCoord.getX();
		double y2 = toNodeCoord.getY();
		return Math.sqrt((y2-y1)*(y2-y1)+(x2-x1)*(x2-x1));
	}

	private double getConstantB(Coord fromNodeCoord, Coord toNodeCoord, double cellCentroidX, double cellCentroidY){
		double x1 = fromNodeCoord.getX();
		double y1 = fromNodeCoord.getY();
		double x2 = toNodeCoord.getX();
		double y2 = toNodeCoord.getY();
		double x0 = cellCentroidX;
		double y0 = cellCentroidY;
		return (x2-x1)*(x1-x0)+(y2-y1)*(y1-y0);
	}


	@Override
	public Double getNormalizationFactor() {
		if(smoothingRadius_m>0.0) return (cellsize_m/smoothingRadiusSquared_m);
		return 1.0;
	}
	
	@Override
	public Double getWeightFromCoord(Coord emittingCoord, Coord receivingCoord) {
		logger.warn("line weight util is not supposed to work with a pair of coordinates. Return 0.0 as weight.");
		return 0.0;
		
	}

}
