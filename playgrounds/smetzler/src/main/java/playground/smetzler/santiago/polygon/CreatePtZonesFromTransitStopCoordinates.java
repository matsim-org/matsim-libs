/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.smetzler.santiago.polygon;

import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * @author aneumann, smetzler
 *
 */
public class CreatePtZonesFromTransitStopCoordinates {
	
	private static final Logger log = Logger.getLogger(CreatePtZonesFromTransitStopCoordinates.class);
	public static final String NAME_IDENTIFIER = "zoneId";

	public static void createPtZonesFromTransitStopCoordinates(String transitStopListFilename, String zoneShapeFilename){
		Map<String, List<Coord>> ptZoneId2TransitStopCoordinates = ReadStopTable.readGenericCSV(transitStopListFilename);
		Map<String, Geometry> ptZoneId2MultiPointGeometry = createMulitipointGeometriesFromTransitStopCoordinates(ptZoneId2TransitStopCoordinates);
		
		Map<String, Polygon> ptZoneId2Polygon = createPolygonFromMultiPointGeometry(ptZoneId2MultiPointGeometry);
		
		writeResultAsShapeToFile(ptZoneId2Polygon, zoneShapeFilename);
	}

	private static Map<String, Geometry> createMulitipointGeometriesFromTransitStopCoordinates(Map<String, List<Coord>> ptZoneId2TransitStopCoordinates) {

		Map<String, Geometry> ptZoneId2MultiPointGeometry = new HashMap<>();

		for (String ptZone : ptZoneId2TransitStopCoordinates.keySet()) {
			ArrayList<Coordinate> jtsCoords = new ArrayList<>();

			for (Coord transitStopCoordinate : ptZoneId2TransitStopCoordinates.get(ptZone)) {
				jtsCoords.add(new Coordinate(transitStopCoordinate.getX(), transitStopCoordinate.getY(), 0.0));
			}

			Coordinate[] coordinates = jtsCoords.toArray(new Coordinate[jtsCoords.size()]);
			Geometry multiPoint = new GeometryFactory().createMultiPoint(coordinates);
			ptZoneId2MultiPointGeometry.put(ptZone, multiPoint);
		}

		return ptZoneId2MultiPointGeometry;
	}

	private static Map<String, Polygon> createPolygonFromMultiPointGeometry(Map<String, Geometry> ptZoneId2MultiPointGeometry) {
		Map<String, Polygon> ptZoneId2Polygon = new HashMap<>();

		for (String ptZoneId : ptZoneId2MultiPointGeometry.keySet()) {
			Geometry multiPointGeometry = ptZoneId2MultiPointGeometry.get(ptZoneId);
				if (multiPointGeometry.getNumPoints() > 2) {
					Polygon convexHull = (Polygon) multiPointGeometry.convexHull();
					ptZoneId2Polygon.put(ptZoneId, convexHull);
				} else {
					// bei weniger als 3 Punkten wird zunaechst kein Polygon sondern
					// ein Punkt oder ein Linestring gemacht. Deswegen wird darum
					// ein Buffer gelegt.
					Geometry convexHullLine = multiPointGeometry.convexHull();
					Polygon buffer = (Polygon) convexHullLine.buffer(20);
					ptZoneId2Polygon.put(ptZoneId, buffer);
				}
		}
		return ptZoneId2Polygon;
	}

	private static void writeResultAsShapeToFile(Map<String, Polygon> ptZoneId2Polygon, String filename) {

		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().
				setCrs(MGC.getCRS("EPSG:24879")).
				setName("keinName").
				addAttribute(CreatePtZonesFromTransitStopCoordinates.NAME_IDENTIFIER, String.class).
				create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();


		Object[] featureAttribs;
		for(Entry<String, Polygon> ptZoneId2PolygonEntry: ptZoneId2Polygon.entrySet()){
			featureAttribs = new Object[1];
			featureAttribs[0] = ptZoneId2PolygonEntry.getKey();
			features.add(factory.createPolygon(ptZoneId2PolygonEntry.getValue(), featureAttribs, null));

		}
		ShapeFileWriter.writeGeometries(features, filename);
		log.info("Shape written to " + filename);
	}

	public static void main(String[] args) throws Exception {
		
		final String directory = "e:/_shared-svn/_data/santiago_pt_demand_matrix/";
		final String transitStopListFilename = directory + "/raw_data/Diccionario paradero-zona777.csv.gz";
		final String zoneShapeFilename = directory + "converted_data/pt_zones.shp";
		
		CreatePtZonesFromTransitStopCoordinates.createPtZonesFromTransitStopCoordinates(transitStopListFilename, zoneShapeFilename);
	}
}