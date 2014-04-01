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
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;
import playground.southafrica.utilities.grid.GeneralGrid;

public class BuildingTypeParser {
	private final static Logger LOG = Logger.getLogger(BuildingTypeParser.class);
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
	private final GeneralGrid grid;
	private Map<String, Map<String, Integer>> activityMap;

	public BuildingTypeParser(String shapefile, double width, int gridType) {
		MyMultiFeatureReader mmfr = new MyMultiFeatureReader();
		try {
			mmfr.readMultizoneShapefile(shapefile, 1);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read area shapefile from " + shapefile);
		}
		this.grid = new GeneralGrid(width, gridType);
		this.grid.generateGrid(mmfr.getAllZones().get(0));
		
		this.activityMap = new HashMap<String, Map<String,Integer>>();
	}
	
	
	public static void main(String[] args) {
		Header.printHeader(BuildingTypeParser.class.toString(), args);
		
		String areaShapefile = args[0];
		Double gridWidth = Double.parseDouble(args[1]);
		Integer gridType = Integer.parseInt(args[2]);
		String buildingShapefile = args[3];
		String outputFolder = args[4];
		
		BuildingTypeParser btp = new BuildingTypeParser(areaShapefile, gridWidth, gridType);
		btp.parseGtiBuildingShapefile(buildingShapefile);
		btp.writeActivityMapToFile(outputFolder + (outputFolder.endsWith("/") ? "" : "/") + btp.grid.getGridType() + "_activityTypes.csv");
		btp.grid.writeGrid(outputFolder);
		
		Header.printFooter();
	}
	
	private void parseGtiBuildingShapefile(String shapefile){
		LOG.info("Reading building features from " + shapefile);
		Collection<SimpleFeature> buildingFeatures = ShapeFileReader.getAllFeatures(shapefile);
		LOG.info("Done reading features.");
		
		LOG.info("Parsing densities from features... (" + buildingFeatures.size() + " features)");
		Counter counter = new Counter("   features # ");
		for(SimpleFeature feature : buildingFeatures){
			Geometry geo = (Geometry) feature.getDefaultGeometry();
			if(geo instanceof Point){
				Point ps = (Point)geo;
				
				/* Transform the coordinate */
				Coordinate coordinate = ps.getCoordinate();
				Coord cOld = new CoordImpl(coordinate.x, coordinate.y);
				Coord cNew = ct.transform(cOld);
				
				String code = feature.getAttribute("S_LU_CODE").toString();
				
				/* Check if the land-use code has already been featured. */
				if(!activityMap.containsKey(code)){
					activityMap.put(code, new HashMap<String, Integer>());
				}
				Map<String, Integer> thisMap = activityMap.get(code);
				
				/* Get the closest cell. */
				Tuple<String, Point> tuple = this.grid.getGrid().get(cNew.getX(), cNew.getY());
				if(!thisMap.containsKey(tuple.getFirst())){
					thisMap.put(tuple.getFirst(), 0);
				}
				
				/* Increase the cell's value. */
				int oldValue = thisMap.get(tuple.getFirst());
				thisMap.put(tuple.getFirst(), oldValue+1);

			} else{
				throw new RuntimeException("The shapefile does not only contain Point(s)!");
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done parsing densities.");
	}
	
	private void writeActivityMapToFile(String filename){
		LOG.info("Writing output to " + filename);
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("Type,Subtype,From,To,Count");
			bw.newLine();
			
			for(String activityType : this.activityMap.keySet()){
				String mainType = activityType.substring(0, activityType.indexOf("."));
				Map<String, Integer> thisMap = this.activityMap.get(activityType);
				for(String cell : thisMap.keySet()){
					String[] sa = cell.split("_");
					bw.write(String.format("%s,%s,%s,%s,%d\n", mainType, activityType, sa[0], sa[1], thisMap.get(cell)));
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
	
	
	
	
	

}
