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
import java.util.Map.Entry;

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
public class DgZonesUtils {
	
	private static final Logger log = Logger.getLogger(DgZonesUtils.class);
	
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
			map.put(zone, link);
		}
		return map;
	}


	/**
	 * Use with care
	 */
	public static void writePolygonZones2Shapefile(List<DgZone> cells, CoordinateReferenceSystem crs, String shapeFilename){
		Collection<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("grid_cell");
		b.add("location", Polygon.class);
		b.add("to_cell_id", String.class);
		b.add("trips", Double.class);
		SimpleFeatureBuilder factory = new SimpleFeatureBuilder(b.buildFeatureType());
			
		try {
			for (DgZone cell : cells){
				log.info("writing cell: " + cell.getId());
				List<Object> attributes = new ArrayList<Object> ();
				Polygon p = cell.getPolygon();
				attributes.add(p);
				for (Entry<DgZone, Double> entry : cell.getToZoneRelations().entrySet()){
					log.info("  to cell " + entry.getKey().getId() + " # trips: " + entry.getValue());
					attributes.add( entry.getKey().getId());
					attributes.add( entry.getValue());
					Object[] atts = attributes.toArray();
					SimpleFeature feature = factory.buildFeature(null, atts);
					featureCollection.add(feature);
					attributes.clear();
					attributes.add(p);
				}
			}		
			ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
		} catch (Exception  e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
	}


	public static void writeLineStringOdPairsFromZones2Shapefile(List<DgZone> cells,
			CoordinateReferenceSystem crs, String shapeFilename) {
		List<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("ls_od_pair");
		b.add("location", LineString.class);
		b.add("no trips", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		
		GeometryFactory geoFac = new GeometryFactory();
		
		for (DgZone zone : cells){
			for (DgDestination destination : zone.getDestinations() ){
				addLineStringFeature(builder, featureCollection, geoFac, zone, destination);
			}
			for (DgZoneFromLink fromLink : zone.getFromLinks().values()){
				for (DgDestination destination : fromLink.getDestinations() ){
					addLineStringFeature(builder, featureCollection, geoFac, fromLink, destination);
				}
			}
		}
		ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
	}
	
	private static void addLineStringFeature(SimpleFeatureBuilder builder, List<SimpleFeature> featureCollection, GeometryFactory geoFac, DgOrigin origin, DgDestination destination) throws IllegalArgumentException{
		Coordinate startCoordinate = origin.getCoordinate();
		Coordinate endCoordinate = destination.getCoordinate();
		Coordinate[] coordinates = {startCoordinate, endCoordinate};
		LineString lineString = geoFac.createLineString(coordinates);
		Object[] atts = {lineString, destination.getNumberOfTrips()};
		SimpleFeature feature = builder.buildFeature(null, atts);
		featureCollection.add(feature);
	}

}
