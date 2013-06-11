/* *********************************************************************** *
 * project: org.matsim.*
 * DgZoneODShapefileWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author dgrether
 * 
 */
public class DgZoneWriter {

	
	private static final Logger log = Logger.getLogger(DgZoneWriter.class);
	
	private CoordinateReferenceSystem crs;
	private DgZones zones;
	private GeometryFactory geoFac;

	public DgZoneWriter(DgZones cells, CoordinateReferenceSystem crs) {
		this.zones = cells;
		this.crs = crs;
		this.geoFac = new GeometryFactory();
	}

	public void writePolygonZones2Shapefile(String shapeFilename){
		Collection<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("grid_cell");
		b.add("location", Polygon.class);
		b.add("zone_id", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		try {
			for (DgZone cell : zones.values()){
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
	
	public void writeLineStringZone2ZoneOdPairsFromZones2Shapefile(String shapeFilename) {
		List<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("zone2destOdPairs");
		b.add("location", LineString.class);
		b.add("from_zone", String.class);
		b.add("to_dest", String.class);
		b.add("no trips", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		int featureId = 1;
		for (DgZone zone : this.zones.values()) {
			for (DgDestination destination : zone.getDestinations()) {
				Coordinate startCoordinate = zone.getCoordinate();
				Coordinate endCoordinate = destination.getCoordinate();
				Coordinate[] coordinates = { startCoordinate, endCoordinate };
				LineString lineString = geoFac.createLineString(coordinates);
				Object[] atts = { lineString, zone.getId(), destination.getId(),
						destination.getNumberOfTrips() };
				SimpleFeature feature = builder.buildFeature(Integer.toString(featureId), atts);
				featureId++;
				featureCollection.add(feature);
			}
		}
		ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
	}

	public void writeLineStringLink2LinkOdPairsFromZones2Shapefile(String shapeFilename) {
		List<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("link2destOdPairs");
		b.add("location", LineString.class);
		b.add("from_link", String.class);
		b.add("to_dest", String.class);
		b.add("no trips", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		int featureId = 1;
		for (DgZone zone : this.zones.values()) {
			for (DgZoneFromLink fromLink : zone.getFromLinks().values()) {
				for (DgDestination destination : fromLink.getDestinations()) {
					Coordinate startCoordinate = fromLink.getCoordinate();
					Coordinate endCoordinate = destination.getCoordinate();
					Coordinate[] coordinates = { startCoordinate, endCoordinate };
					LineString lineString = geoFac.createLineString(coordinates);
					Object[] atts = { lineString, fromLink.getLink().getId().toString(), destination.getId(),
							destination.getNumberOfTrips() };
					SimpleFeature feature = builder.buildFeature(Integer.toString(featureId), atts);
					featureId++;
					featureCollection.add(feature);
				}
			}
			if (! featureCollection.isEmpty()) {
				ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
			}
		}
	}
}