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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.jjoubert.CommercialTraffic.SAZone;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MyZoneReader {

	private final Logger log = Logger.getLogger(MyZoneReader.class);
	private String shapefile;
	private List<MyZone> zoneList;
	private Map<Id, MyZone> zoneMap;
	private QuadTree<SAZone> quadTree;

	private double xMin = Double.POSITIVE_INFINITY;
	private double xMax = Double.NEGATIVE_INFINITY;
	private double yMin = Double.POSITIVE_INFINITY;
	private double yMax = Double.NEGATIVE_INFINITY;
	
	
	public MyZoneReader(String shapefile){
		this.shapefile = shapefile;
		
		File file = new File(shapefile);
		if(file.exists()){
		this.shapefile = shapefile;
		} else{
			throw new RuntimeException("The shapefile " + shapefile + " does not exist!!");
		}
	}
	
//	private void buildQuadTree() {
//		log.info("Building QuadTree<SAZone> from read mesozones.");
//		this.quadTree = new QuadTree<SAZone>(this.xMin, this.yMin, this.xMax, this.yMax);
//		for (SAZone zone : this.zones) {
//			this.quadTree.put(zone.getCentroid().getX(), zone.getCentroid().getY(), zone); 
//		}
//		log.info("QuadTree<SAZone> completed.");
//	}

	/**
	 * Read the shapefile. Known Id fields:
	 * <ul>
	 * 		<li> eThekwini transport zones - 1
	 * 		<li> eThekwini sub-place - 2
	 * 		<li> eThekwini parcel - 1
	 * 		<li> Gauteng GAP zones - 1
	 * 		<li> KwazuluNatal GAP - 2
	 * 		<li> Western Cape GAP - 2
	 * 		<li> MATSim test - 1 
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public void readZones (int idField){
		log.info("Reading shapefile " + this.shapefile);		
		this.zoneList = new ArrayList<MyZone>();
		this.zoneMap = new HashMap<Id, MyZone>();
		FeatureSource fs = null;
		MultiPolygon mp = null;
		try {	
			fs = ShapeFileReader.readDataFile( this.shapefile );
			Collection<Object> objectArray = (ArrayList<Object>) fs.getFeatures().getAttribute(0);
			for (Object o : objectArray) {
				String name = String.valueOf(((Feature) o).getAttribute(idField));
				Geometry shape = ((Feature) o).getDefaultGeometry();
				if( shape instanceof MultiPolygon ){
					mp = (MultiPolygon)shape;
					if( !mp.isSimple() ){
						log.warn("This polygon is NOT simple!" );
					}
					if(mp.getNumGeometries() > 1){
						log.warn("MultiPolygon " + name + " has more than one polygon.");
					}
					Polygon[] polygonArray = new Polygon[mp.getNumGeometries()];
					for(int j = 0; j < mp.getNumGeometries(); j++ ){
						if(mp.getGeometryN(j) instanceof Polygon ){
							polygonArray[j] = (Polygon) mp.getGeometryN(j);							
						} else{
							log.warn("Subset of multipolygon is NOT a polygon.");
						}
					}
					MyZone newZone = new MyZone(polygonArray, mp.getFactory(), new IdImpl(name));
					zoneList.add(newZone);
					zoneMap.put(newZone.getId(), newZone);

					/*
					 * Determine the extent of the QuadTree.
					 */
					Point centroid = newZone.getCentroid();
					this.xMin = Math.min(centroid.getX(), this.xMin);
					this.xMax = Math.max(centroid.getX(), this.xMax);
					this.yMin = Math.min(centroid.getY(), this.yMin);
					this.yMax = Math.max(centroid.getY(), this.yMax);
				} else{
					log.warn("This is not a multipolygon!");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		log.info("Done reading " + shapefile);
	}
	
//	public SAZone getZone(String id){
//		SAZone result = null;
//		for (SAZone aZone : this.zones) {
//			if(aZone.getName().equalsIgnoreCase(id)){
//				result = aZone;
//			}
//		}
//		if(result == null){
//			String errorMessage = "Could not find the requested mesozone: " + id;
//			log.error(errorMessage);
//		}
//		return result;
//	}
//	
//	public ArrayList<SAZone> getAllZones() {
//		return this.zones;
//	}
	
	public QuadTree<SAZone> getGapQuadTree() {
		return quadTree;
	}
	
	public List<MyZone> getZoneList(){
		return this.zoneList;
	}
	
	public Map<Id,MyZone> getZoneMap(){
		return this.zoneMap;
	}

	public String getShapefileName() {
		return this.shapefile;
	}


	


}
