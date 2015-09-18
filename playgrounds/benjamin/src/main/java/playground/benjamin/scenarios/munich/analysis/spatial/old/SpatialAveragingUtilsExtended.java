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
package playground.benjamin.scenarios.munich.analysis.spatial.old;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

/**
 * @author amit
 */
public class SpatialAveragingUtilsExtended {

	private final Logger logger = Logger.getLogger(SpatialAveragingUtils.class);
	double smoothinRadiusSquared_m;

	public SpatialAveragingUtilsExtended(double smoothingRadius_m) {
		this.smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	}

	//V1 results in negative weights. Will remove it later. Amit
	public double calculateWeightOfLineForCellV1(Coord fromNodeCoord, Coord toNodeCoord, double cellCentroidX, double cellCentroidY) throws MathException {
		double constantA = getConstantA(fromNodeCoord, cellCentroidX, cellCentroidY);
		double constantB = getConstantB(fromNodeCoord, toNodeCoord, cellCentroidX, cellCentroidY);
		double constantC =0.;
		double linkLengthFromCoord = getLinkLengthUsingFromAndToNode(fromNodeCoord, toNodeCoord);
		double constantR = Math.sqrt(smoothinRadiusSquared_m);

		constantC= Math.exp(-(constantA-(constantB*constantB/(linkLengthFromCoord*linkLengthFromCoord)))/smoothinRadiusSquared_m);
		double upperLimit = (linkLengthFromCoord+(constantB/linkLengthFromCoord));
		double lowerLimit = constantB/(linkLengthFromCoord);
		double integrationUpperLimit = (-smoothinRadiusSquared_m/(2*linkLengthFromCoord))*Math.exp(-upperLimit*upperLimit/smoothinRadiusSquared_m) - (constantR*constantB*Math.sqrt(Math.PI))/(linkLengthFromCoord*linkLengthFromCoord*2)*Erf.erf(upperLimit/constantR);
		double integrationLowerLimit = (-smoothinRadiusSquared_m/(2*linkLengthFromCoord))*Math.exp(-lowerLimit*lowerLimit/smoothinRadiusSquared_m) - (constantR*constantB*Math.sqrt(Math.PI))/(linkLengthFromCoord*linkLengthFromCoord*2)*Erf.erf(lowerLimit/constantR);
		double  weight = constantC *(integrationUpperLimit-integrationLowerLimit);
		
		if(weight<0.0) {
			logger.warn("weight is negative, please check. Weight = "+weight);
		}
		return weight;
	}
	public double calculateWeightOfLineForCellV2(Coord fromNodeCoord, Coord toNodeCoord, double cellCentroidX, double cellCentroidY) {
		double constantA = getConstantA(fromNodeCoord, cellCentroidX, cellCentroidY);
		double constantB = getConstantB(fromNodeCoord, toNodeCoord, cellCentroidX, cellCentroidY);
		double constantC =0.;
		double linkLengthFromCoord = getLinkLengthUsingFromAndToNode(fromNodeCoord, toNodeCoord);
		double constantR = Math.sqrt(smoothinRadiusSquared_m);
		constantC= (constantR*(Math.sqrt(Math.PI))/(linkLengthFromCoord*2))*Math.exp(-(constantA-(constantB*constantB/(linkLengthFromCoord*linkLengthFromCoord)))/smoothinRadiusSquared_m);

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
			logger.warn("Weight is negative, please check. Weight = "+weight);
		}
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
		return (x2-x1)*(x1-x0)+(y2-y1)*(y1-y0);
	}
}
