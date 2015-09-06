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

package playground.southafrica.utilities.gis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MyMultiFeatureReader {

	private List<MyZone> zones;
	private final static Logger LOG = Logger.getLogger(MyMultiFeatureReader.class);
	public MyMultiFeatureReader(){

	}
	
	
	/**
	 * Reads multizone shapefiles.
	 * @param shapefile
	 * @param idField indicates which field to use for the {@link Id} of the zone. Known fields include:
	 * <ul>
	 * 	<li>Geospatial Analysis Platform (GAP) shapefiles:
	 * 	<ul>
	 * 		<li> Gauteng: 1
	 * 		<li> KwaZulu-Natal: 2
	 * 		<li> Western Cape: 2
	 * 	</ul>
	 * </ul>
	 * @throws IOException if the shapefile does not exist, or is not readable.
	 */
	public void readMultizoneShapefile (String shapefile, int idField) throws IOException{
		File f = new File(shapefile);
		if(!f.exists() || !f.canRead()){
			throw new IOException("Cannot read from " + shapefile);
		}
		LOG.info("Start reading shapefile " + shapefile);

		this.zones = new ArrayList<MyZone>();
		MultiPolygon mp = null;
		GeometryFactory gf = new GeometryFactory();
		
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		
		for (SimpleFeature feature : features) {
			String name = null;
			
			Object o = feature.getAttribute(idField);
			name = o.toString();
			/* Removed the code below (June 2014, JWJ) since it became 
			 * increasingly more complex to check different class types. Just
			 * use whatever Id field is given. It is the user's responsibility 
			 * to check whether it makes sense. */
//			if(o instanceof String){
//				name = (String)o;
//			} else if(o instanceof Double){
//				name = String.valueOf(((Double)o).intValue());
//			} else if(o instanceof Integer){
//				name = String.valueOf((Integer)o);
//			} else if(o instanceof Long){
//				name = String.valueOf((Long)o);
//			} else{
//				LOG.error("Don't know how to interpret ID field type: " + o.getClass().toString());
//			}
			
			Object shape = feature.getDefaultGeometry();
			if( shape instanceof MultiPolygon ){
				mp = (MultiPolygon)shape;
				if( !mp.isSimple() ){
					LOG.warn("This polygon is NOT simple: " + name);
				}
				Polygon polygonArray[] = new Polygon[mp.getNumGeometries()];
				for(int j = 0; j < mp.getNumGeometries(); j++ ){
					if(mp.getGeometryN(j) instanceof Polygon ){
						polygonArray[j] = (Polygon) mp.getGeometryN(j);							
					} else{
						LOG.warn("Subset of multipolygon is NOT a polygon.");
					}
				}
				MyZone newZone = new MyZone(polygonArray, gf, Id.create(name, MyZone.class));

				this.zones.add( newZone );
			} else{
				LOG.warn("This is not a multipolygon!");
			}
		}
		LOG.info("Done reading shapefile.");
	}

	public MyZone getZone(Id id){
		MyZone result = null;
		for (MyZone aZone : this.zones) {
			if(aZone.getId().equals(id)){
				result = aZone;
			}
		}
		if(result == null){
			String errorMessage = "Could not find the requested zone: " + id.toString();
			LOG.error(errorMessage);
		}
		return result;
	}
	
	
	public List<MyZone> getAllZones() {
		return this.zones;
	}
	
	
	/**
	 * Reads a point feature shapefile.
	 * @return {@link List} of {@link Point}s.
	 */
	public List<Point> readPoints(String shapefile) {
		List<Point> list = new ArrayList<Point>();
		for(SimpleFeature feature : ShapeFileReader.getAllFeatures(shapefile)){
			Geometry geo = (Geometry) feature.getDefaultGeometry();
			if(geo instanceof Point){
				Point ps = (Point)geo;
				
				for(int i = 0; i < ps.getNumGeometries(); i++){
					Point p = (Point) ps.getGeometryN(i);
					list.add(p);
				}
			} else{
				throw new RuntimeException("The shapefile does not contain Point(s)!");
			}
		}
		return list;
	}


	/**
	 * Reads a point feature shapefile.
	 * @return {@link List} of {@link Coord}s.
	 */
	public List<Coord> readCoords(String shapefile) {
		List<Coord> list = new ArrayList<Coord>();
		ShapeFileReader sfr = new ShapeFileReader();
		Collection<SimpleFeature> features = sfr.readFileAndInitialize(shapefile);
		for(SimpleFeature feature: features){
			Geometry geo = (Geometry) feature.getDefaultGeometry();
			if(geo instanceof Point){
				Point ps = (Point)geo;
				list.add(new Coord(ps.getX(), ps.getY()));
			} else{
				throw new RuntimeException("The shapefile does not contain Point(s)!");
			}
		}
		return list;
	}
	
	
	/**
	 * Implementation of the shapefile reader to check which fields contains the
	 * zone id.
	 * @param args
	 */
	public static void main(String[] args){
		Header.printHeader(MyMultiFeatureReader.class.toString(), args);
		
		MyMultiFeatureReader mr = new MyMultiFeatureReader();
		try {
			mr.readMultizoneShapefile(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot parse multi-feature shapefile.");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot parse multi-feature shapefile.");
		}
		
		/* Report the Ids of the first ten entries... */
		LOG.info("The zone Ids of the first ten (or fewer) entries:");
		for(int i = 0; i < Math.min(10, mr.getAllZones().size()); i++){
			LOG.info("   " + mr.getAllZones().get(i).getId().toString());
		}
		LOG.info("   :");
		LOG.info("   :");
		LOG.info("   :");
		LOG.info("   :_");
		
		LOG.info("If this doesn't makes sense, then you've got the wrong ID field, probably ;-)");
		Header.printFooter();
	}
	


}
