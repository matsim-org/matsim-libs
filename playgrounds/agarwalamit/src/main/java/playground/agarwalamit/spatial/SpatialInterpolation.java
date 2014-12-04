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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;
import org.matsim.api.core.v01.network.Link;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A class to interpolate effect of emissions on each link on other parts of study area.
 * @author amit
 */

public class SpatialInterpolation {

	public SpatialInterpolation() {
		new File(SpatialDataInputs.outputDir).mkdirs();

		this.grid = createGridFromBoundingBox();
		this.grid.writeGrid(SpatialDataInputs.outputDir, SpatialDataInputs.targetCRS.toString());

		// initialize map
		this.cellWeights = new HashMap<Point, Double>(this.grid.getGrid().size());
		for(Point p :this.grid.getGrid().values()){
			this.cellWeights.put(p, 0.);
		}
	}

	public static void main(String[] args) {

	}

	private GeometryFactory gf;
	private GeneralGrid grid ;

	/**
	 * Point (key of map) could be centroid of hexagonal grid or square grid
	 */
	public Map<Point, Double> cellWeights;

	/**
	 * @return general grid of cells from pre defined bounding box
	 */
	private GeneralGrid createGridFromBoundingBox (){
		gf = new GeometryFactory();
		// create polygon from bounding box and then get polygon from that
		Coordinate c1 = new Coordinate(SpatialDataInputs.xMin,SpatialDataInputs.yMin);
		Coordinate c2 = new Coordinate(SpatialDataInputs.xMax,SpatialDataInputs.yMin);
		Coordinate c3 = new Coordinate(SpatialDataInputs.xMin,SpatialDataInputs.yMax);
		Coordinate c4 = new Coordinate(SpatialDataInputs.xMax,SpatialDataInputs.yMax);

		Polygon polygon = gf.createPolygon(new Coordinate []{c1,c2,c3,c4});

		GeneralGrid generalGrid = new GeneralGrid(SpatialDataInputs.cellWidth, SpatialDataInputs.gridType);
		generalGrid.generateGrid(polygon);

		return generalGrid;
	}

	/**
	 * @param link
	 * @param intensityOnLink (emissions, ...) intensity for 100% sample.
	 */
	public void processLink(Link link, double intensityOnLink){
		for(Point p: this.cellWeights.keySet()){

			Coordinate pointCoord = p.getCoordinate();
			Coordinate linkCentroid = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			Coordinate fromNodeCoord = new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY());
			Coordinate toNodeCoord = new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY());

			double cellArea = this.grid.getCellGeometry(p).getArea();
			double normalizationFactor = cellArea/SpatialDataInputs.boundingBoxArea;
			double weightSoFar = this.cellWeights.get(p);
			double weightNow;


			if(SpatialDataInputs.linkWeigthMethod.equals("line")){

				weightNow = calculateWeightFromLine(fromNodeCoord,toNodeCoord,pointCoord) * normalizationFactor;

			} else if(SpatialDataInputs.linkWeigthMethod.equals("point")) {

				weightNow = calculateWeightFromPoint(linkCentroid, pointCoord) * normalizationFactor;

			} else throw new RuntimeException("Averaging method for weight is not recongnized. Use 'line' or 'point'.");

			this.cellWeights.put(p, weightNow+weightSoFar);
		}
	}

	/**
	 * @param fromNodeCoord
	 * @param toNodeCoord
	 * @param cellCentroid
	 * @return The outcome is derived assuming constant emission on link and then integrating effect of emission on link on the cell centroid.
	 */
	private double calculateWeightFromLine(Coordinate fromNodeCoord,Coordinate toNodeCoord, Coordinate cellCentroid){
		double constantA = fromNodeCoord.distance(cellCentroid) * fromNodeCoord.distance(cellCentroid);
		double constantB = (toNodeCoord.x-fromNodeCoord.x)*(fromNodeCoord.x-cellCentroid.x) + 
				(toNodeCoord.y-fromNodeCoord.y)*(fromNodeCoord.y-cellCentroid.y);
		double constantC = 0.;
		double linkLengthFromCoord = fromNodeCoord.distance(toNodeCoord);
		double constantR = SpatialDataInputs.smoothingRadius;

		constantC= (constantR*(Math.sqrt(Math.PI))/(linkLengthFromCoord*2))*Math.exp(-(constantA-(constantB*constantB/(linkLengthFromCoord*linkLengthFromCoord)))/constantR*constantR);

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
			throw new RuntimeException("Weight is negative, eeight = "+weight+ ". Thus aborting.");
		}
		return weight;
	}


	private double calculateWeightFromPoint(Coordinate linkCentroid, Coordinate cellCentroid){
		double dist = linkCentroid.distance(cellCentroid);
		double smoothingRadius_square = SpatialDataInputs.smoothingRadius * SpatialDataInputs.smoothingRadius;
		double weight = Math.exp((- dist * dist) / (smoothingRadius_square));
		return weight;
	}

}