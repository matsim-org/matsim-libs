/* *********************************************************************** *
 * project: org.matsim.*
 * MyGapReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.jfree.util.Log;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.jjoubert.CommercialModel.MyCommercialControlerV01;
import playground.jjoubert.CommercialTraffic.SAZone;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MyGapReader {

	private String areaName;
	private int gapIdField;
	private String shapefile;
	private ArrayList<SAZone> zones;
	private final static Logger LOG = Logger.getLogger(MyCommercialControlerV01.class);
	private QuadTree<SAZone> gapQuadTree;

	private double xMin = Double.POSITIVE_INFINITY;
	private double xMax = Double.NEGATIVE_INFINITY;
	private double yMin = Double.POSITIVE_INFINITY;
	private double yMax = Double.NEGATIVE_INFINITY;
	
	
	public MyGapReader(String area, String filename){
		this.areaName = area;

		/*
		 * GAP_Id field index for the different provinces:
		 * 			 Gauteng: 1
		 * 		    	 KZN: 2
		 *  	Western Cape: 2
		 *  			  SA: ?
		 */
		if(this.areaName.equalsIgnoreCase("Gauteng")){
			this.gapIdField = 1;
		} else if(this.areaName.equalsIgnoreCase("KZN") ||
				  this.areaName.equalsIgnoreCase("WesternCape")){
			this.gapIdField = 2;
		} else{
			throw new RuntimeException("The given area name does not have a known ID field!!");
		}
		
		File file = new File(filename);
		if(file.exists()){
		this.shapefile = filename;
		} else{
			throw new RuntimeException("The shapefile " + filename + " does not exist!!");
		}
		
		readGapShapefile();
		buildQuadTree();
	}
	
	private void buildQuadTree() {
		LOG.info("Building QuadTree<SAZone> from read mesozones.");
		this.gapQuadTree = new QuadTree<SAZone>(this.xMin, this.yMin, this.xMax, this.yMax);
		for (SAZone zone : this.zones) {
			this.gapQuadTree.put(zone.getCentroid().getX(), zone.getCentroid().getY(), zone); 
		}
		LOG.info("QuadTree<SAZone> completed.");
	}

	@SuppressWarnings("unchecked")
	private void readGapShapefile (){
		String startMessage = "Start reading shapefile for " + this.areaName;
		LOG.info(startMessage);
		
		this.zones = new ArrayList<SAZone>();
		FeatureSource fs = null;
		MultiPolygon mp = null;
		GeometryFactory gf = new GeometryFactory();
		try {	
			fs = ShapeFileReader.readDataFile( this.shapefile );
			ArrayList<Object> objectArray = (ArrayList<Object>) fs.getFeatures().getAttribute(0);
			for (int i = 0; i < objectArray.size(); i++) {
				Object thisZone = objectArray.get(i);
				// For GAP files, field [1] contains the GAP_ID
				String name = String.valueOf( ((Feature) thisZone).getAttribute( this.gapIdField ) ); 
				Geometry shape = ((Feature) thisZone).getDefaultGeometry();
				if( shape instanceof MultiPolygon ){
					mp = (MultiPolygon)shape;
					if( !mp.isSimple() ){
						LOG.warn("This polygon is NOT simple!" );
					}
					Polygon polygonArray[] = new Polygon[mp.getNumGeometries()];
					for(int j = 0; j < mp.getNumGeometries(); j++ ){
						if(mp.getGeometryN(j) instanceof Polygon ){
							polygonArray[j] = (Polygon) mp.getGeometryN(j);							
						} else{
							LOG.warn("Subset of multipolygon is NOT a polygon.");
						}
					}
					SAZone newZone = new SAZone(polygonArray, gf, name, 0);
					
					this.zones.add( newZone );
					/*
					 * Determine the extent of the QuadTree.
					 */
					Point centroid = newZone.getCentroid();
					this.xMin = Math.min(centroid.getX(), this.xMin);
					this.xMax = Math.max(centroid.getX(), this.xMax);
					this.yMin = Math.min(centroid.getY(), this.yMin);
					this.yMax = Math.max(centroid.getY(), this.yMax);
				} else{
					LOG.warn("This is not a multipolygon!");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		LOG.info("Done reading shapefile.");
	}
	
	public SAZone getZone(String id){
		SAZone result = null;
		for (SAZone aZone : this.zones) {
			if(aZone.getName().equalsIgnoreCase(id)){
				result = aZone;
			}
		}
		if(result == null){
			String errorMessage = "Could not find the requested mesozone: " + id;
			LOG.error(errorMessage);
		}
		return result;
	}
	
	public ArrayList<SAZone> getAllZones() {
		return this.zones;
	}
	
	public QuadTree<SAZone> getGapQuadTree() {
		return gapQuadTree;
	}


	


}
