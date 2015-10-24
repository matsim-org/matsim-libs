/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import org.matsim.visum.VisumMatrixWriter;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class MatrixExtractDE {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Matrix m1 = new Matrix("1", null);
		VisumMatrixReader reader = new VisumMatrixReader(m1);
		reader.readFile("/home/johannes/gsv/matrices/IV_gesamt.O.fma");

		ZoneLayer<Map<String, Object>> zones = ZoneLayerSHP.read("/home/johannes/gsv/matrices/zones_zone.SHP");
		Map<String, String> gsv2nuts = new HashMap<String, String>();
		for (Zone<Map<String, Object>> zone : zones.getZones()) {
			gsv2nuts.put(zone.getAttribute().get("NO").toString(), (String) zone.getAttribute().get("CODE"));
		}

		Matrix m2 = new Matrix("2", null);

		Set<String> ids = new HashSet<>(m1.getFromLocations().keySet());
		ids.addAll(m1.getToLocations().keySet());

		for (String from : ids) {
			if (gsv2nuts.get(from).startsWith("DE")) {
				for (String to : ids) {
					if (gsv2nuts.get(to).startsWith("DE")) {
						Entry e = m1.getEntry(from, to);
						if (e != null) {
							m2.createEntry(from, to, e.getValue());
						}
					}
				}
			}
		}
//		/*
//		 * divide intracells by 2
//		 */
//		for(String id : ids) {
//			Entry e = m2.getEntry(id, id);
//			if(e != null) {
//				e.setValue(e.getValue() / 2.0);
//			}
//		}
		
		VisumMatrixWriter writer = new VisumMatrixWriter(m2);
		writer.writeFile("/home/johannes/gsv/matrices/netz2030.fma");
	}

}
