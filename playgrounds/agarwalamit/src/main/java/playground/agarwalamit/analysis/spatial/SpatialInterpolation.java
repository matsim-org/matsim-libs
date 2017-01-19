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
package playground.agarwalamit.analysis.spatial;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.utils.GeometryUtils;

/**
 * A class to interpolate effect of emissions (or ...) on each link on other parts of study area.
 * @author amit
 */

public class SpatialInterpolation {

	private GeometryFactory gf;
	private GeneralGrid grid ;
	private Polygon boundingBoxPolygon;
	private final SpatialDataInputs inputs;

	private Map<Point, Double> cellWeights;
	private final String outputFolder ;
	private final boolean isFilteringCellForShape;
	private Collection<Geometry> geoms;

	public SpatialInterpolation(final SpatialDataInputs inputs, final String outputFolder) {
		this(inputs, outputFolder, false);
	}

	public SpatialInterpolation(final SpatialDataInputs inputs, final String outputFolder, final boolean isFilteringCellForShape) {
		this.inputs = inputs;
		SpatialDataInputs.LOG.info("Creating grids inside polygon of bounding box.");
		createGridFromBoundingBox();

		this.outputFolder = outputFolder;
		if(!new File(this.outputFolder).exists()) new File(this.outputFolder).mkdirs();

		this.isFilteringCellForShape = isFilteringCellForShape;
		if( this.isFilteringCellForShape ){
			if(inputs.shapeFile!=null){
				Collection<SimpleFeature> sfs = new ShapeFileReader().readFileAndInitialize(inputs.shapeFile);
				geoms = GeometryUtils.getSimplifiedGeometries(sfs);
			} else throw new RuntimeException("Cell filtering is on but no shape file is provided. Aborting ...");
		}
		this.grid.writeGrid(outputFolder, inputs.targetCRS.toString());
		reset();
	}

	/**
	 * Used to clear the cell weights map.
	 */
	public final void reset(){
		this.cellWeights = new HashMap<>();

		this.grid.getGrid().values()
				 .stream()
				 .filter(p->isCellIncludedForInterpolation(p))
				 .forEach( p -> this.cellWeights.put(p,0.));

		if (isFilteringCellForShape) SpatialDataInputs.LOG.warn(this.grid.getGrid().size() - this.cellWeights.size() + " cells are removed from bounding box.");
	}

	/**
	 * general grid of cells from pre defined bounding box
	 */
	private void createGridFromBoundingBox (){
		gf = new GeometryFactory();
		// create polygon from bounding box and then get polygon from that
		Coordinate c1 = new Coordinate(inputs.getxMin(),inputs.getyMin());
		Coordinate c2 = new Coordinate(inputs.getxMax(),inputs.getyMin());
		Coordinate c3 = new Coordinate(inputs.getxMin(),inputs.getyMax());
		Coordinate c4 = new Coordinate(inputs.getxMax(),inputs.getyMax());

		boundingBoxPolygon = gf.createPolygon(new Coordinate []{c1,c2,c4,c3,c1}); // sequence to create polygon is equally important

		this.grid = new GeneralGrid(inputs.getCellWidth(), inputs.getGridType());
		this.grid.generateGrid(boundingBoxPolygon);

		SpatialDataInputs.LOG.warn("Total number of cells in the grid are "+this.grid.getGrid().size());
	}

	/**
	 * @param link
	 * @param intensityOnLink (emissions etc) intensity for 100% sample.
	 */
	public void processLink(final Link link, final double intensityOnLink){

		Coordinate linkCentroid = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
		Coordinate fromNodeCoord = new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY());
		Coordinate toNodeCoord = new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY());

		for(Point p: this.cellWeights.keySet()){
			
			double cellArea = this.grid.getCellGeometry(p).getArea();
			double areaSmoothingCircle = Math.PI * inputs.getSmoothingRadius() * inputs.getSmoothingRadius();
			double normalizationFactor = cellArea/areaSmoothingCircle;
			double weightNow = 0;

			switch (inputs.getLinkWeigthMethod()) {

			case line :
				weightNow = intensityOnLink * calculateWeightFromLine(fromNodeCoord,toNodeCoord, p.getCoordinate()) * normalizationFactor;
				break;
			case point : 
				weightNow = intensityOnLink * calculateWeightFromPoint(linkCentroid, p.getCoordinate()) * normalizationFactor;
				break;
			default:
				throw new RuntimeException("Averaging method for weight is not recongnized. Use 'line' or 'point'.");
			}
			double weightSoFar = this.cellWeights.get(p);
			this.cellWeights.put(p, weightNow+weightSoFar);
		}
	}

	/**
	 * @param act actType
	 * @param intensityOfPoint (userWelfare, personToll etc) intensity for 100% sample.
	 */
	public void processHomeLocation(final Activity act, final double intensityOfPoint){

		Coordinate actCoordinate = new Coordinate (act.getCoord().getX(),act.getCoord().getY());

		for(Point p: this.cellWeights.keySet()){
			double cellArea = this.grid.getCellGeometry(p).getArea();
			double areaSmoothingCircle = Math.PI * inputs.getSmoothingRadius() *inputs.getSmoothingRadius();
			double normalizationFactor = cellArea/areaSmoothingCircle;
			double weightNow;

			switch(inputs.getLinkWeigthMethod()){
			case point :
				weightNow = intensityOfPoint * calculateWeightFromPoint(actCoordinate, p.getCoordinate()) * normalizationFactor;
				break;
			default : 
				throw new RuntimeException("Averaging method other than point method is not possible for a point intensity. Aborting ...");
			}
			double weightSoFar = this.cellWeights.get(p);
			this.cellWeights.put(p, weightNow + weightSoFar);
		}
	}

	/**
     */
	public void processLocationForDensityCount(final Activity act, final double countScaleFactor){

		Coordinate actCoordinate = new Coordinate (act.getCoord().getX(),act.getCoord().getY());
		Point actLocation = gf.createPoint(actCoordinate);

		this.cellWeights.keySet().stream().filter(p -> this.grid.getCellGeometry(p).covers(actLocation)).forEach(p -> {
			this.cellWeights.put(p, this.cellWeights.get(p) + 1 * countScaleFactor);
		});
	}

	/**
	 * @param fromNodeCoord
	 * @param toNodeCoord
	 * @param cellCentroid
	 * @return The outcome is derived assuming constant emission on link and then integrating effect of emission on link on the cell centroid.
	 */
	private double calculateWeightFromLine(final Coordinate fromNodeCoord, final Coordinate toNodeCoord, final Coordinate cellCentroid){
		double constantA = fromNodeCoord.distance(cellCentroid) * fromNodeCoord.distance(cellCentroid);
		double constantB = (toNodeCoord.x-fromNodeCoord.x)*(fromNodeCoord.x-cellCentroid.x) + 
				(toNodeCoord.y-fromNodeCoord.y)*(fromNodeCoord.y-cellCentroid.y);
		double constantC = 0.;
		double linkLengthFromCoord = fromNodeCoord.distance(toNodeCoord);
		double constantR = inputs.getSmoothingRadius();

		constantC= (constantR*(Math.sqrt(Math.PI))/(linkLengthFromCoord*2))*Math.exp(-(constantA-(constantB*constantB/(linkLengthFromCoord*linkLengthFromCoord)))/(constantR*constantR));

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
		} else if (Double.isNaN(weight)) weight = 0;
		return weight;
	}


	private double calculateWeightFromPoint(final Coordinate linkCentroid, final Coordinate cellCentroid){
		double dist = linkCentroid.distance(cellCentroid);
		double smoothingRadiusSquare = inputs.getSmoothingRadius() * inputs.getSmoothingRadius();
		return Math.exp((- dist * dist) / (smoothingRadiusSquare));
	}

	/**
	 * Point (key of map) could be centroid of hexagonal grid or square grid
	 */
	public Map<Point, Double> getCellWeights(){
		return this.cellWeights;
	}

	public GeneralGrid getGrid() {
		return grid;
	}

	/**
	 * @param fileNamePrefix
	 * @param writingGGPLOTData if true, will write one row for each vertex of polygon else 
	 * will write all vertices in a row
	 * 
	 * Same data sheet can be used for plotting surface polygon and filled contour plot data. 
	 * <p> The file name will composed of grid type, cell width, link weight method and scenario seperated by "_".
	 */
	public void writeRData(final String fileNamePrefix, final boolean writingGGPLOTData){
		SpatialDataInputs.LOG.info("====Writing data to plot polygon surface in R.====");

		GridType type = inputs.getGridType();

		String fileName;
		if(inputs.isComparing){
			String scenarioCase = inputs.compareToCase.split("/") [inputs.compareToCase.split("/").length-1];
			fileName = outputFolder+"/rData"+"_"+fileNamePrefix+"_"+type+"_"+inputs.getCellWidth()+"_"+inputs.getLinkWeigthMethod()+"_"+scenarioCase+"_"+"diff"+".txt";
		} else {
			String scenarioCase = inputs.getInitialCase().split("/") [inputs.getInitialCase().split("/").length-1];
			fileName= outputFolder+"/rData"+"_"+fileNamePrefix+"_"+type+"_"+inputs.getCellWidth()+"_"+inputs.getLinkWeigthMethod()+"_"+scenarioCase+".txt";	
		}

		int noOfSidesOfPolygon = 0;
		if(type.equals(GridType.SQUARE)) noOfSidesOfPolygon = 4;
		else if(type.equals(GridType.HEX)) noOfSidesOfPolygon = 6;
		else throw new RuntimeException(type +" is not a valid grid type.");

		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			if(writingGGPLOTData){
				writer.write("polyNr \t x \t y \t centroidX \t centroidY \t weight \n");
				int polyNr = 1;
				for(Point p : this.cellWeights.keySet()){
					Geometry g = this.grid.getCellGeometry(p);
					Coordinate[] ca = g.getCoordinates();
					for(int i = 0; i < ca.length-1; i++){ // a square polygon have 5 coordinate, first and last is same. 
						writer.write(polyNr+"\t"+ca[i].x+"\t"+ca[i].y+"\t"+p.getX()+"\t"+p.getY()+"\t"+this.cellWeights.get(p)+"\n");
					}
					polyNr++;
				}
			} else {
				for(int i=0;i<noOfSidesOfPolygon;i++){
					writer.write("polyX"+i+"\t"+"polyY"+i+"\t");
				}
				writer.write("centroidX \t centroidY \t weight \n ");

				for(Point p : this.cellWeights.keySet()){
					Geometry g = this.grid.getCellGeometry(p);
					Coordinate[] ca = g.getCoordinates();
					for(int i = 0; i < ca.length-1; i++){ // a square polygon have 5 coordinate, first and last is same. 
						writer.write(ca[i].x+"\t"+ca[i].y+"\t");
					}
					double thisWeight = this.cellWeights.get(p);
					writer.write(p.getX()+"\t"+p.getY()+"\t"+thisWeight+"\n");
				}
			}
			writer.close();
			SpatialDataInputs.LOG.info("Data is written to file "+fileName);
		} catch (IOException e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
	}

	public boolean isInResearchArea(final Link link) {
		Coordinate linkCentroid = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
		Point linkCentroidPoint = gf.createPoint(linkCentroid);
		return boundingBoxPolygon.covers(linkCentroidPoint);
	}
	
	private boolean isCellIncludedForInterpolation(final Point point){
		if( this.isFilteringCellForShape ) {
			for(Geometry g : geoms){
				Geometry pointGeom =  gf.createPoint( new Coordinate( point.getCoordinate() ) );
				if(g.contains(pointGeom)) return true;
			}
		} else return true;
		return false;
	}
}