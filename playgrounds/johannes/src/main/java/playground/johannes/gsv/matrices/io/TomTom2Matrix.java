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

package playground.johannes.gsv.matrices.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.sna.util.ProgressLogger;

/**
 * @author johannes
 * 
 */
public class TomTom2Matrix {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Matrix m = new Matrix("tomtom", null);
		VisumMatrixReader reader = new VisumMatrixReader(m);

		reader.readFile("/home/johannes/gsv/matrices/raw/TomTom/TTgrob_gesamt_aus_zeitunabhängig.txt");

		ZoneCollection zones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.json")));
		zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		data = null;
		
		Set<Entry> all = new HashSet<>();
		for (List<Entry> entries : m.getFromLocations().values()) {
			all.addAll(entries);
		}

		for (List<Entry> entries : m.getToLocations().values()) {
			all.addAll(entries);
		}

		KeyMatrix km = new KeyMatrix();

		ProgressLogger.init(all.size(), 2, 10);
		for (Entry e : all) {
			ProgressLogger.step();
			Zone zi = zones.get(e.getFromLocation());
			Zone zj = zones.get(e.getToLocation());
			if(zi != null && zj != null) {
			km.set(e.getFromLocation(), e.getToLocation(), e.getValue());
			}
		}

		KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
		writer.write(km, "/home/johannes/gsv/matrices/refmatrices/tomtom.de.xml");
	}

}
