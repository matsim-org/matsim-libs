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
package playground.agarwalamit.spatial;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.benjamin.scenarios.munich.analysis.nectar.SpatialAveragingUtils;

/**
 * @author amit
 */
public class SpatialAveragingUtilsExtended {
	
	private static final Logger logger = Logger.getLogger(SpatialAveragingUtils.class);
	double smoothinRadiusSquared_m;
	
	public SpatialAveragingUtilsExtended(double smoothingRadius_m) {
		this.smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	}

	public double calculateWeightOfLineForCell(Coord fromNodeCoord, Coord toNodeCoord, double cellCentroidX, double cellCentroidY) throws MathException {
		double constantA = getConstantA(fromNodeCoord, cellCentroidX, cellCentroidY);
		double constantB = getConstantB(fromNodeCoord, toNodeCoord, cellCentroidX, cellCentroidY);
		double constantRSquare = smoothinRadiusSquared_m;
		double  weight =  	Math.exp(-constantA/constantRSquare)*Math.sqrt(constantRSquare/constantB)*(Erf.erf(Math.sqrt(constantB/constantRSquare))-Erf.erf(0));
		return weight;
	}
	
	double getConstantA(Coord fromNodeCoord, double cellCentroidX, double cellCentroidY){
		double x1 = fromNodeCoord.getX();
		double y1= fromNodeCoord.getY();
		double x0 = cellCentroidX;
		double y0 = cellCentroidY;
		return (x1-x0)*(x1-x0)+(y1-y0)*(y1-y0);
	}
	
	double getLinkLengthUsingFromAndToNode(Coord fromNodeCoord, Coord toNodeCoord){
		double x1 = fromNodeCoord.getX();
		double y1 = fromNodeCoord.getY();
		double x2 = toNodeCoord.getX();
		double y2 = toNodeCoord.getY();
		return Math.sqrt((y2-y1)*(y2-y1)+(x2-x1)*(x2-x1));
	}
	
	double getConstantB(Coord fromNodeCoord, Coord toNodeCoord, double cellCentroidX, double cellCentroidY){
		double x1 = fromNodeCoord.getX();
		double y1 = fromNodeCoord.getY();
		double x2 = toNodeCoord.getX();
		double y2 = toNodeCoord.getY();
		double x0 = cellCentroidX;
		double y0 = cellCentroidY;
		double distBetweenFromAndToNode = getLinkLengthUsingFromAndToNode(fromNodeCoord, toNodeCoord);
		return distBetweenFromAndToNode*distBetweenFromAndToNode+2*(x2-x1)*(y2-y1)*(x1-x0)*(y1-y0);
	}
}
