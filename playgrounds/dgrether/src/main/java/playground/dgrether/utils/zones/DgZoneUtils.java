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
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
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
	
	public static DgZones createZonesFromGrid(DgGrid grid){
		int idint = 1000;
		DgZones zones= new DgZones();
		for (Polygon p : grid){
			idint++;
			Id<DgZone> id = Id.create(Integer.toString(idint), DgZone.class);
			DgZone cell = new DgZone(id, p);
			zones.put(id, cell);
			log.info("Envelope of cell " + id + " is " + cell.getEnvelope());
		}
		return zones;
	}

	
	public static void createZoneCenter2LinkMapping(DgZones zones, Network network){
		for (DgZone zone : zones.values()){
			Coord coord = MGC.coordinate2Coord(zone.getCoordinate());
			final Coord coord1 = coord;
			Link link = NetworkUtils.getNearestLinkExactly(network,coord1);
			if (link == null) throw new IllegalStateException("No nearest link found");
			zone.setZoneNetworkConnectionLink(link);
		}
	}


	

	public static void writeLinksOfZones2Shapefile(DgZones cells, 	CoordinateReferenceSystem crs, String shapeFilename){
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
			for (DgZone zone : cells.values()){
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
				Link l = zone.getZoneNetworkConnectionLink();
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
