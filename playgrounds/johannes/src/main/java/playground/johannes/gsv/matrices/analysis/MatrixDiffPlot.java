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

package playground.johannes.gsv.matrices.analysis;

import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import playground.johannes.gsv.matrices.MatrixOperations;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class MatrixDiffPlot {

	public static ZoneLayer<Map<String, Object>> diffLayer(Matrix m1, Matrix m2, ZoneLayer<Map<String, Object>> zonelayer) {
		zonelayer.overwriteCRS(CRSUtils.getCRS(4326));
		Set<Zone<Map<String, Object>>> zones = new HashSet<>();

		for (Zone<Map<String, Object>> zone : zonelayer.getZones()) {
			String code = zone.getAttribute().get("ISO_CODE").toString();
			if (code.equalsIgnoreCase("DE")) {
				String id = zone.getAttribute().get("NO").toString();

				double fromSum1 = sum(m1.getFromLocEntries(id), false);
				double fromSum2 = sum(m2.getFromLocEntries(id), false);

				Zone<Map<String, Object>> newZone = new Zone<>(zone.getGeometry());
				zones.add(newZone);
				newZone.setAttribute(new HashMap<String, Object>());
				double err = (fromSum2 - fromSum1) / fromSum1;
				newZone.getAttribute().put("SOURCE_ERR", err);
				

				double toSum1 = sum(m1.getToLocEntries(id), false);
				double toSum2 = sum(m2.getToLocEntries(id), false);
				err = (toSum2 - toSum1) / toSum1;

				newZone.getAttribute().put("TARGET_ERR", err);
				newZone.getAttribute().put("LABEL", String.format("%.2f / %.2f / %.2f", err, toSum1, toSum2));
				
				zones.add(newZone);
			}
		}

		ZoneLayer<Map<String, Object>> newLayer = new ZoneLayer<>(zones);
		newLayer.overwriteCRS(CRSUtils.getCRS(4326));

		return newLayer;
	}

	private static double sum(List<Entry> entries, boolean ignoreIntracell) {
		double sum = 0;
		if (entries != null) {
			for (Entry e : entries) {
				if (ignoreIntracell) {
					if (!e.getFromLocation().equalsIgnoreCase(e.getToLocation())) {
						sum += e.getValue();
					}
				} else {
					sum += e.getValue();
				}
			}
		}
		return sum;
	}

	public static void main(String args[]) throws IOException {
		Matrix m1 = new Matrix("2", null);
		VisumMatrixReader reader = new VisumMatrixReader(m1);
		reader.readFile("/home/johannes/gsv/matrices/itp.fma");

		Matrix m2 = new Matrix("2", null);
		reader = new VisumMatrixReader(m2);
		reader.readFile("/home/johannes/gsv/matrices/miv.365.fma");

		MatrixOperations.applyFactor(m1, 1 / 365.0);
		MatrixOperations.applyFactor(m2, 12);
		MatrixOperations.applyIntracellFactor(m2, 1.3);

		ZoneLayer<Map<String, Object>> zonelayer = ZoneLayerSHP.read("/home/johannes/gsv/matrices/zones_zone.SHP");

		ZoneLayer<Map<String, Object>> newLayer = diffLayer(m1, m2, zonelayer);

		ZoneLayerSHP.writeWithAttributes(newLayer, "/home/johannes/gsv/matrices/diff.itp.365.shp");
	}
}
