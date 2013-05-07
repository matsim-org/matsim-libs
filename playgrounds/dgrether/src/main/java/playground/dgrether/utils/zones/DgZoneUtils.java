/* *********************************************************************** *
 * project: org.matsim.*
 * DgZonesUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.utils.zones;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.utils.DgGrid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;


/**
 * @author dgrether
 *
 */
public class DgZoneUtils {
	
	private static final Logger log = Logger.getLogger(DgZoneUtils.class);
	
	public static List<DgZone> createZonesFromGrid(DgGrid grid){
		int id = 1000;
		List<DgZone> cells = new ArrayList<DgZone>();
		for (Polygon p : grid){
			id++;
			DgZone cell = new DgZone(Integer.toString(id), p);
			cells.add(cell);
			log.info("Envelope of cell " + id + " is " + cell.getEnvelope());
		}
		return cells;
	}

	
	public static Map<DgZone, Link> createZoneCenter2LinkMapping(List<DgZone> zones, NetworkImpl network){
		Map<DgZone, Link> map = new HashMap<DgZone, Link>();
		for (DgZone zone : zones){
			Coord coord = MGC.coordinate2Coord(zone.getCoordinate());
			Link link = network.getNearestLinkExactly(coord);
			if (link == null) throw new IllegalStateException("No nearest link found");
			map.put(zone, link);
		}
		return map;
	}


	public static void writePolygonZones2Shapefile(List<DgZone> cells, CoordinateReferenceSystem crs, String shapeFilename){
		Collection<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("grid_cell");
		b.add("location", Polygon.class);
		b.add("zone_id", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		try {
			for (DgZone cell : cells){
				log.info("writing cell: " + cell.getId());
				List<Object> attributes = new ArrayList<Object> ();
				Polygon p = cell.getPolygon();
				attributes.add(p);
				attributes.add(cell.getId());
				Object[] atts = attributes.toArray();
				SimpleFeature feature = builder.buildFeature(cell.getId().toString(), atts);
				attributes.clear();
				featureCollection.add(feature);
			}		
			ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
		} catch (Exception  e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
	}

	public static void writeLinksOfZones2Shapefile(List<DgZone> cells, Map<DgZone, Link> zones2LinkMap, 
			CoordinateReferenceSystem crs, String shapeFilename){
		List<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		GeometryFactory geoFac = new GeometryFactory();
		SimpleFeatureTypeBuilder linkFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
		linkFeatureTypeBuilder.setCRS(crs);
		linkFeatureTypeBuilder.setName("link");
		linkFeatureTypeBuilder.add("location", LineString.class);
		linkFeatureTypeBuilder.add("zone", String.class);
		linkFeatureTypeBuilder.add("is_center_link", Boolean.class);
		SimpleFeatureBuilder linkBuilder = new SimpleFeatureBuilder(linkFeatureTypeBuilder.buildFeatureType());
		int featureId = 1;
		try {
			for (DgZone zone : cells){
				for (DgZoneFromLink link : zone.getFromLinks().values()){
					Link l = link.getLink();
					Coordinate startCoordinate = MGC.coord2Coordinate(l.getFromNode().getCoord());
					Coordinate endCoordinate = MGC.coord2Coordinate(l.getToNode().getCoord());
					Coordinate[] coordinates = {startCoordinate, endCoordinate};
					LineString lineString = geoFac.createLineString(coordinates);
					Object[] atts = {lineString, zone.getId().toString(), false};
					SimpleFeature linkFeature = linkBuilder.buildFeature(Integer.toString(featureId), atts);
					featureId++;
					featureCollection.add(linkFeature);
				}
				Link l = zones2LinkMap.get(zone);
				Coordinate startCoordinate = MGC.coord2Coordinate(l.getFromNode().getCoord());
				Coordinate endCoordinate = MGC.coord2Coordinate(l.getToNode().getCoord());
				Coordinate[] coordinates = {startCoordinate, endCoordinate};
				LineString lineString = geoFac.createLineString(coordinates);
				Object[] atts = {lineString, zone.getId().toString(), true};
				SimpleFeature linkFeature = linkBuilder.buildFeature(Integer.toString(featureId), atts);
				featureId++;
				featureCollection.add(linkFeature);
				
			}		
			ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
		} catch (Exception  e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 

	}
	



}
