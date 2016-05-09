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

import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/matrices/refmatrices/tomtom.xml");
		NumericMatrix m = reader.getMatrix();

		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.json")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
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
