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

package playground.johannes.gsv.matrices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.linear.MatrixIndexException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSON;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOpertaions;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.ODMatrixXMLReader;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 * 
 */
public class RelationsDiffPlot {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String runId = "629";
		String simFile = String.format("/home/johannes/gsv/matrices/simmatrices/miv.%s.xml", runId);
		String refFile2 = "/home/johannes/gsv/matrices/refmatrices/bmbv2010.xml";
		String refFile1 = "/home/johannes/gsv/matrices/refmatrices/itp.xml";
		/*
		 * load ref matrix
		 */
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(refFile1);
		KeyMatrix reference1 = reader.getMatrix();

		MatrixOpertaions.applyFactor(reference1, 1 / 365.0);

		reader.parse(refFile2);
		KeyMatrix reference2 = reader.getMatrix();

		MatrixOpertaions.applyFactor(reference2, 1 / 365.0);
		
		/*
		 * load simulated matrix
		 */
		reader.parse(simFile);
		KeyMatrix simulation = reader.getMatrix();

		MatrixOpertaions.sysmetrize(simulation);
		MatrixOpertaions.applyFactor(simulation, 11.0);
		MatrixOpertaions.applyDiagonalFactor(simulation, 1.3);
		/*
		 * load zones
		 */
		ZoneCollection zones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/de.nuts3.json")));
		zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		/*
		 * compare
		 */
		data = writeGeoJSON(zones, urbanZones(zones.zoneSet()), simulation, reference1, reference2);
		Files.write(Paths.get("/home/johannes/gsv/matrices/analysis/matrixdiff."+runId+".json"), data.getBytes(), StandardOpenOption.CREATE);
	}

	private static String writeGeoJSON(ZoneCollection zones, Collection<Zone> relationZones, KeyMatrix m1, KeyMatrix m2, KeyMatrix m3) {
		StringBuilder builder = new StringBuilder();
		/*
		 * write zone polygons
		 */
		// builder.append(Zone2GeoJSON.toJson(zones.zoneSet()));
		/*
		 * create a line string for each relation
		 */
		GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		GeoJSONWriter jsonWriter = new GeoJSONWriter();
		List<Feature> features = new ArrayList<>();

		for (Zone i : relationZones) {
			for (Zone j : relationZones) {
				if (i != j) {
					Double val1 = m1.get(i.getAttribute("gsvId"), j.getAttribute("gsvId"));
					if (val1 == null)
						val1 = 0.0;
					Double val2 = m2.get(i.getAttribute("gsvId"), j.getAttribute("gsvId"));
					if (val2 == null)
						val2 = 0.0;

					Double val3 = m3.get(i.getAttribute("gsvId"), j.getAttribute("gsvId"));
					if (val3 == null)
						val3 = 0.0;
					
					double err1 = (val1 - val2) / val2;
					double err2 = (val1 - val3) / val3;

					Point start = i.getGeometry().getCentroid();
					Point end = j.getGeometry().getCentroid();

					Coordinate[] coords = new Coordinate[2];
					coords[0] = start.getCoordinate();
					coords[1] = end.getCoordinate();

					double deltaX = coords[1].x - coords[0].x;
					double deltaY = coords[1].y - coords[0].y;
					
					double rotation = Math.atan(deltaY/deltaX) * 180/Math.PI;
					
//					coords[1].x = coords[0].x + deltaX/2.0;
//					coords[1].y = coords[0].y + deltaY/2.0;
					
//					double labelX = coords[0].x + deltaX/4.0;
//					double labelY = coords[0].y + deltaY/4.0;
					
					double labelX = coords[0].x + deltaX/2.0;
					double labelY = coords[0].y + deltaY/2.0;
					
					LineString line = factory.createLineString(coords);

					GeoJSON json = jsonWriter.write(line);
					Geometry geom = (Geometry) GeoJSONFactory.create(json.toString());

					Map<String, Object> atts = new HashMap<>();
					atts.put("volume", val1);
					atts.put("error1", err1);
					atts.put("error2", err2);
					atts.put("labelX", labelX);
					atts.put("labelY", labelY);
					atts.put("rotation", rotation);
					features.add(new Feature(geom, atts));
				}
			}
		}

		builder.append(jsonWriter.write(features).toString());

		return builder.toString();
	}

	private static Collection<Zone> urbanZones(Collection<Zone> zones) {
		final double threshold = 600000;

		Set<Zone> urbanZones = new HashSet<>();

		for (Zone zone : zones) {
			double pop = Double.parseDouble(zone.getAttribute("inhabitants"));
			double a = zone.getGeometry().getArea();

			double rho = pop / a;

			if (pop > threshold) {
				urbanZones.add(zone);
			}
		}

		return urbanZones;
	}
}
