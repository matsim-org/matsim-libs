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

import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

		reader.readFile(args[0]);

		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get(args[1])));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		zones.setPrimaryKey("NO");
		data = null;

		Set<Entry> all = new HashSet<>();
		for (List<Entry> entries : m.getFromLocations().values()) {
			all.addAll(entries);
		}

		for (List<Entry> entries : m.getToLocations().values()) {
			all.addAll(entries);
		}

		NumericMatrix km = new NumericMatrix();

		ProgressLogger.init(all.size(), 2, 10);
		for (Entry e : all) {
			ProgressLogger.step();
			Zone zi = zones.get(e.getFromLocation());
			Zone zj = zones.get(e.getToLocation());
			if (zi != null && zj != null) {
				km.set(e.getFromLocation(), e.getToLocation(), e.getValue());
			}
		}

		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(km, args[2]);
	}

}
