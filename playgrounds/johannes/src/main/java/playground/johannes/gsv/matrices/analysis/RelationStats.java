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

package playground.johannes.gsv.matrices.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 * 
 */
public class RelationStats {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/matrices/refmatrices/tomtom.xml");
		KeyMatrix m = reader.getMatrix();

		ZoneCollection zones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.json")));
		zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		data = null;

		SortedSet<Double> values = new TreeSet<>();

		DistanceCalculator dCalc = new WGS84DistanceCalculator();
		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				Zone zone_i = zones.get(i);
				Zone zone_j = zones.get(j);

				if (zone_i != null && zone_j != null) {
					Point pi = zone_i.getGeometry().getCentroid();
					Point pj = zone_j.getGeometry().getCentroid();

					double d = dCalc.distance(pi, pj);

					if (d >= 100000) {
						Double val = m.get(i, j);
						if (val != null && val >= 100) {
							values.add(val);
							
//							if(i.equals("11000") && j.equals("9162")) {
//								System.out.println("found: " + val);
//							}
						}
					}
				}
			}
		}
		System.out.println(values);
		System.out.println(String.format("%s relations, max = %s, min = %s", values.size(), values.last(), values.first()));
	}

}
