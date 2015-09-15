/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.complexNetworks.gtiLandUse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;
import playground.southafrica.utilities.grid.KernelDensityEstimator;
import playground.southafrica.utilities.grid.KernelDensityEstimator.KdeType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class BuildingTypeParser extends AbstractAnalysis{
	private final static Logger LOG = Logger.getLogger(BuildingTypeParser.class);
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
	private final GeneralGrid grid;
	private final double width;
	private Map<String, KernelDensityEstimator> activityMap;

	public BuildingTypeParser(String shapefile, double width, GridType gridType) {
		MyMultiFeatureReader mmfr = new MyMultiFeatureReader();
		try {
			mmfr.readMultizoneShapefile(shapefile, 1);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read area shapefile from " + shapefile);
		}
		this.width = width;
		this.grid = new GeneralGrid(width, gridType);
		this.grid.generateGrid(mmfr.getAllZones().get(0));
		
		this.activityMap = new HashMap<String, KernelDensityEstimator>();
	}
	
	
	public static void main(String[] args) {
		Header.printHeader(BuildingTypeParser.class.toString(), args);
		
		String areaShapefile = args[0];
		GridType gridType = GridType.valueOf(args[1]);
		Double gridWidth = Double.parseDouble(args[2]);
		String buildingShapefile = args[3];
		String outputFolder = args[4];
		
		BuildingTypeParser btp = new BuildingTypeParser(areaShapefile, gridWidth, gridType);
		btp.parseGtiBuildingShapefile(buildingShapefile);
		btp.writeActivityMapToFile(String.format("%s%s%s_activityTypes_%.0f.csv", outputFolder, (outputFolder.endsWith("/") ? "" : "/"), btp.grid.getGridType(), gridWidth));
		btp.grid.writeGrid(outputFolder, "WGS84_SA_Albers");
		
		/* Visualise. */
		try {
			AnalysisLauncher.open(btp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot visualise!!");
		}

		Header.printFooter();
	}
	
	private void parseGtiBuildingShapefile(String shapefile){
		LOG.info("Reading building features from " + shapefile);
		Collection<SimpleFeature> buildingFeatures = ShapeFileReader.getAllFeatures(shapefile);
		LOG.info("Done reading features.");
		
		LOG.info("Parsing densities from features... (" + buildingFeatures.size() + " features)");
		for(SimpleFeature feature : buildingFeatures){
			Geometry geo = (Geometry) feature.getDefaultGeometry();
			if(geo instanceof Point){
				Point ps = (Point)geo;
				
				/* Transform the coordinate */
				Coordinate coordinate = ps.getCoordinate();
				Coord cOld = new Coord(coordinate.x, coordinate.y);
				Coord cNew = ct.transform(cOld);
				
				String code = feature.getAttribute("S_LU_CODE").toString();
				
				/* Check if the land-use code has already been featured. */
				if(!activityMap.containsKey(code)){
					activityMap.put(code, new KernelDensityEstimator(this.grid, KdeType.CELL, this.width));
				}
				KernelDensityEstimator kde = activityMap.get(code);
				kde.processPoint(ps.getFactory().createPoint(new Coordinate(cNew.getX(), cNew.getY())), 1.0);
			} else{
				throw new RuntimeException("The shapefile does not only contain Point(s)!");
			}
		}
		LOG.info("Done parsing densities.");
	}
	
	
	private void writeActivityMapToFile(String filename){
		LOG.info("Writing output to " + filename);
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("Type,Subtype,X,Y,Count");
			bw.newLine();
			
			for(String activityType : this.activityMap.keySet()){
				String mainType = activityType.substring(0, activityType.indexOf("."));
				KernelDensityEstimator kde = this.activityMap.get(activityType);
				for(Point cell : kde.getGrid().getGrid().values()){
					double weight = kde.getWeight(cell);
					if(weight > 0.0){
						bw.write(String.format("%s,%s,%.4f,%.4f,%.4f\n", mainType, activityType, cell.getX(), cell.getY(), weight));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Done writing output.");
	}


	@Override
	public void init() throws Exception {
		chart = AWTChartComponentFactory.chart(Quality.Intermediate, "awt");
		Coord3d newViewPoint = new Coord3d(3*Math.PI/2, Math.PI/2, 10*width);
		chart.getView().setViewPoint(newViewPoint, true);
		chart.getView().setViewPositionMode(ViewPositionMode.FREE);

		/* First draw the original geometry. */
		org.jzy3d.plot3d.primitives.Polygon polygon = new org.jzy3d.plot3d.primitives.Polygon();
		for(Coordinate c : this.grid.getGeometry().getCoordinates()){
			polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(c.x, c.y, 0.0)));
		}
		polygon.setWireframeDisplayed(true);
		polygon.setWireframeColor(Color.BLACK);
		
		chart.getScene().add(polygon);
		
		for(Point p : this.grid.getGrid().values()){
			org.jzy3d.plot3d.primitives.Polygon cell = new org.jzy3d.plot3d.primitives.Polygon();
			Geometry g = this.grid.getCellGeometry(p);
			Coordinate[] ca = g.getCoordinates();
			for(int i = 0; i < ca.length-1; i++){
				cell.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(ca[i].x, ca[i].y, 0.0)));
			}
			cell.setFaceDisplayed(false);
			cell.setWireframeDisplayed(true);
			cell.setWireframeColor(Color.BLACK);
			chart.getScene().add(cell);
		}
		
		chart.getView().shoot();
		
	}

	
	
	
	
	

}
