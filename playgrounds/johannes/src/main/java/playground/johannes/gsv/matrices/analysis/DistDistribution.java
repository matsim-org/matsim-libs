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
import gnu.trove.TDoubleDoubleHashMap;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.sna.math.DescriptivePiStatistics;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;

import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class DistDistribution {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/sge/prj/matsim/run/874/output/matrices-averaged/miv.sym.xml");
//		reader.parse("/home/johannes/gsv/miv-matrix/refmatrices/tomtom.de.modena.xml");
		KeyMatrix m = reader.getMatrix();

		ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON("/home/johannes/gsv/gis/modena/geojson/zones.gk3.geojson", "NO");
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		DescriptivePiStatistics stats = new DescriptivePiStatistics();

		Set<String> keys = m.keys();
		for (String i : keys) {
			Zone zi = zones.get(i);
			if ("DE".equalsIgnoreCase(zi.getAttribute("ISO_CODE"))) {
				Point pi = zi.getGeometry().getCentroid();
				for (String j : keys) {
					Zone zj = zones.get(j);
					if ("DE".equalsIgnoreCase(zj.getAttribute("ISO_CODE"))) {
						Double val = m.get(i, j);
						if (val != null) {
							Point pj = zj.getGeometry().getCentroid();
							double d = dCalc.distance(pi, pj);
							stats.addValue(d, 1/val);
						}
					}
				}
			}
		}

		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(25000), true);
		Histogram.normalize(hist);
//		TXTWriter.writeMap(hist, "d", "p", "/home/johannes/gsv/miv-matrix/analysis/distances/tomtom.dist.txt");
		TXTWriter.writeMap(hist, "d", "p", "/home/johannes/gsv/miv-matrix/analysis/distances/874.dist.txt");
	}

}
