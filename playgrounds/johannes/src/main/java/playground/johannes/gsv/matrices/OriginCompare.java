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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOpertaions;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;

/**
 * @author johannes
 * 
 */
public class OriginCompare {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String runId = "593";
		String simFile = String.format("/home/johannes/gsv/matrices/simmatrices/miv.%s.xml", runId);
		String refFile = "/home/johannes/gsv/matrices/refmatrices/itp.xml";
		/*
		 * load ref matrix
		 */
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(refFile);
		KeyMatrix m1 = reader.getMatrix();

		MatrixOpertaions.applyFactor(m1, 1 / 365.0);

		/*
		 * load simulated matrix
		 */
		reader.parse(simFile);
		KeyMatrix m2 = reader.getMatrix();

		MatrixOpertaions.applyFactor(m2, 22.0);
		MatrixOpertaions.applyDiagonalFactor(m2, 1.3);
		/*
		 * load zones
		 */
		ZoneCollection zones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/de.nuts3.json")));
		zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		/*
		 * 
		 */
		String originId = "11000";
		/*
		 * get distance matrix
		 */
		KeyMatrix mDist = MatrixCompare2.distanceMatrix(m1, zones);
		Map<String, Double> dists = mDist.getRow(originId);
		dists = sortByValue(dists);
		/*
		 * get errors
		 */
		Map<String, Double> diffs = getDiffVolumes(originId, m1, m2);

		for(Entry<String, Double> entry : dists.entrySet()) {
			Zone zone = zones.get(entry.getKey());
			Double err = diffs.get(entry.getKey());
			System.out.println(String.format("%s: error = %s, distance = %s", zone.getAttribute("nuts3_name"), err, entry.getValue()));
		}
	}

	private static Map<String, Double> getDiffVolumes(String origin, KeyMatrix ref, KeyMatrix sim) {
		Map<String, Double> values = new HashMap<>();

		Map<String, Double> refRow = ref.getRow(origin);
		Map<String, Double> simRow = sim.getRow(origin);

		for (Entry<String, Double> entry : refRow.entrySet()) {
			Double refVal = entry.getValue();
			Double simVal = simRow.get(entry.getKey());
			if (simVal == null)
				simVal = 0.0;

			double err = (simVal - refVal) / refVal;
			values.put(entry.getKey(), err);
		}

		return values;
	}

	private static Map<String, Double> sortByValue(Map<String, Double> map) {
		List<Map.Entry<String, Double>> entries = new LinkedList<>(map.entrySet());

		Collections.sort(entries, new Comparator<Map.Entry<String, Double>>() {

			@Override
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				int result = o1.getValue().compareTo(o2.getValue());
				if (result == 0) {
					return o1.hashCode() - o2.hashCode();
				} else {
					return result;
				}
			}
		});
		
		Map<String, Double> sorted = new LinkedHashMap<>();
		for(Map.Entry<String, Double> entry : entries) {
			sorted.put(entry.getKey(), entry.getValue());
		}
		
		return sorted;
	}

}
