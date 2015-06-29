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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class SpatialAveragingWriter {

	private CoordinateReferenceSystem targetCRS;
	private int noOfXbins;
	private int noOfYbins;
	private Double yMin;
	private Double yMax;
	private Double xMin;
	private Double xMax;
	private boolean useVisBoundary = false;
	
	Collection<SimpleFeature> featuresInVisBoundary;

	private static final Logger logger = Logger.getLogger(SpatialAveragingWriter.class);
	
	public SpatialAveragingWriter(double xMin, double xMax, double yMin, double yMax, int noOfXbins, int noOfYbins, double smoothingRadius_m, 
//			String munichShapeFile, 
			CoordinateReferenceSystem targetCRS, boolean useVisBoundary){
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
		this.noOfXbins = noOfXbins;
		this.noOfYbins = noOfYbins;
//		this.featuresInVisBoundary = ShapeFileReader.getAllFeatures(munichShapeFile);
		this.targetCRS = targetCRS;
		this.useVisBoundary=useVisBoundary;
	}
	
	public SpatialAveragingWriter(SpatialAveragingInputData inputData, int noOfXbins, int noOfYbins, double smoothingRadius_m, boolean useVisBoundary) {
		this(	inputData.getMinX(),
				inputData.getMaxX(), 
				inputData.getMinY(),
				inputData.getMaxY(), 
				noOfXbins, noOfYbins, 
				smoothingRadius_m, 
//				inputData.getMunichShapeFile(),
				inputData.getTargetCRS(), 
				useVisBoundary);
	}

	public SpatialAveragingWriter(SpatialAveragingInputData inputData,
			SpatialAveragingParameters parameters) {
		this(inputData, parameters.getNoOfXbins(), parameters.getNoOfYbins(), parameters.getSmoothingRadius_m(), parameters.IsUsingVisBoundary());
	}

	public SpatialAveragingWriter(SpatialAveragingInputData inputData,
			SpatialAveragingParameters sap, boolean useVisBoundary) {
		this(inputData, sap.getNoOfXbins(), sap.getNoOfYbins(), sap.getSmoothingRadius_m(), useVisBoundary);
	}

	public void writeRoutput(Double[][] doubles, String outputPathForR) {
		try {
			BufferedWriter buffW = new BufferedWriter(new FileWriter(outputPathForR));
			String valueString = new String();
			valueString = "\t";

			//x-coordinates as first row
			for(int xIndex = 0; xIndex < doubles.length; xIndex++){
				valueString += findBinCenterX(xIndex) + "\t";
			}
			buffW.write(valueString);
			buffW.newLine();
			valueString = new String();

			for(int yIndex = 0; yIndex < doubles[0].length; yIndex++){
				//y-coordinates as first column
				valueString += findBinCenterY(yIndex) + "\t";
				//table contents
				for(int xIndex = 0; xIndex < doubles.length; xIndex++){ 
					Coord cellCentroid = findCellCentroid(xIndex, yIndex);
					if(isInVisBoundary(cellCentroid)){
						valueString += Double.toString(doubles[xIndex][yIndex]) + "\t"; 
					} else {
						valueString += "NA" + "\t";
					}
				}
				buffW.write(valueString);
				buffW.newLine();
				valueString = new String();
			}
			buffW.close();	
		} catch (IOException e) {
			throw new RuntimeException("Failed writing output for R. Reason: " + e);
		}	
		logger.info("Finished writing output for R to " + outputPathForR);
	}

	void writeGISoutput(Double[][] doubles, String outputPathForGIS, Double endTimeOfInterval) throws IOException {
		
		PointFeatureFactory factory = new PointFeatureFactory.Builder()
		.setCrs(this.targetCRS)
		.setName("EmissionPoint")
		.addAttribute("Time", String.class)
		.addAttribute("Emissions", Double.class)
		.create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		
//			if(endOfTimeInterval < Time.MIDNIGHT){ // time manager in QGIS does not accept time beyond midnight...
				for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						if(isInVisBoundary(cellCentroid)){
							String dateTimeString = convertSeconds2dateTimeFormat(endTimeOfInterval);
							double result = doubles[xIndex][yIndex];
							SimpleFeature feature = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()), new Object[] {dateTimeString, result}, null);
							features.add(feature);
						}
					}
				}
//			}
		
		ShapeFileWriter.writeGeometries(features, outputPathForGIS);
		logger.info("Finished writing output for GIS to " + outputPathForGIS);
	}

	private double findBinCenterY(int yIndex) {
		double yBinCenter = yMin + ((yIndex + .5) / noOfYbins) * (yMax - yMin);
		return yBinCenter ;
	}

	private double findBinCenterX(int xIndex) {
		double xBinCenter = xMin + ((xIndex + .5) / noOfXbins) * (xMax - xMin);
		return xBinCenter ;
	}
	
	private boolean isInVisBoundary(Coord cellCentroid) {
		if(useVisBoundary==false)return true;
		boolean isInMunichShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.featuresInVisBoundary){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}
	
	public Coord findCellCentroid(int xIndex, int yIndex) {
		double xCentroid = findBinCenterX(xIndex);
		double yCentroid = findBinCenterY(yIndex);
		Coord cellCentroid = new CoordImpl(xCentroid, yCentroid);
		return cellCentroid;
	}
	
	private String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String date = "2012-04-13 ";
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMM);
		String dateTimeString = date + time;
		return dateTimeString;
	}
}
