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
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

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
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/miv-matrix/refmatrices/tomtom.de.xml");
		NumericMatrix m = reader.getMatrix();

		ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON("/home/johannes/gsv/gis/nuts/ger/geojson/psmobility.geojson", "NO", null);
		DistanceCalculator dCalc = new WGS84DistanceCalculator();
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

		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(50000), true);
		Histogram.normalize(hist);
		StatsWriter.writeHistogram(hist, "d", "p", "/home/johannes/gsv/matrix2014/mid-fusion/tomtom.dist.txt");
	}

}
