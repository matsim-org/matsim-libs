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

package playground.johannes.gsv.matrices.postprocess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;

/**
 * @author johannes
 * 
 */
public class Aggregate2Nuts3 {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/matrices/analysis/marketShares/rail.all.xml");
//		reader.parse("/home/johannes/sge/prj/matsim/run/819/output/matrices-averaged/miv.xml");
		KeyMatrix m = reader.getMatrix();

		// ZoneCollection nuts3Zones = new ZoneCollection();
		// String data = new
		// String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.json")));
		// nuts3Zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
		// nuts3Zones.setPrimaryKey("nuts3_code");

		ZoneCollection modenaZones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/modena/zones.gk3.geojson")));
		modenaZones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
		modenaZones.setPrimaryKey("NO");
		data = null;

		Map<String, String> idMap = ZoneIDMappings.modena2gsv2008("/home/johannes/gsv/matrices/refmatrices/modena2gsv2008.txt");

		int cnt = 0;
		for (Zone zone : modenaZones.zoneSet()) {
			String id = zone.getAttribute("NO");
			String gsvId = idMap.get(id);
			if (gsvId != null) {
				zone.setAttribute("gsv2008", gsvId);
			} else {
				if(zone.getAttribute("NUTS3_CODE").startsWith("DE")) {
					zone.setAttribute("gsv2008", "unknownDE" + cnt);
				} else {
					zone.setAttribute("gsv2008", "unknownEU" + cnt);
				}
				cnt++;
			}
		}

		System.out.println("No mappings found for " + cnt + " zones");
		
		m = MatrixOperations.aggregate(m, modenaZones, "gsv2008");

//		KeyMatrix nuts3Matrix = new KeyMatrix();
//		Set<String> keys = m.keys();
//		for (String i : keys) {
//			String i_gsv = idMap.get(i);
//			if (i_gsv == null) {
//				System.err.println(String.format("No mapping found for %s", i));
//			} else {
//				for (String j : keys) {
//
//					String j_gsv = idMap.get(j);
//
//					if (j_gsv != null) {
//						nuts3Matrix.set(i_gsv, j_gsv, m.get(i, j));
//					} else {
//						System.err.println(String.format("No mapping found for %s", j));
//					}
//				}
//			}
//		}

		MatrixOperations.symetrize(m);
		KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
		writer.write(m, "/home/johannes/gsv/matrices/analysis/marketShares/rail.all.nuts3.xml");
//		writer.write(m, "/home/johannes/sge/prj/matsim/run/819/output/matrices-averaged/miv.sym.nuts3.xml");
	}

}
