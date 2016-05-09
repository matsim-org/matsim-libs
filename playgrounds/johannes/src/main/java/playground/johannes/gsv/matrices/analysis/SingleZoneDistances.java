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
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
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
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author johannes
 * 
 */
public class SingleZoneDistances {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/matrices/simmatrices/miv.695.xml");
		NumericMatrix m = reader.getMatrix();

		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.gk3.geojson")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		data = null;

		DescriptivePiStatistics stats = new DescriptivePiStatistics();
		
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		String i = "11000";
		Zone source = zones.get(i);
		for (String j : m.keys()) {
			Double val = m.get(i, j);
			if (val != null) {
				Zone target = zones.get(j);
				if(target != null) {
					Point pi = source.getGeometry().getCentroid();
					Point pj = target.getGeometry().getCentroid();
					
					double d = dCalc.distance(pi, pj);
					stats.addValue(d, 1/val);
				}
			}
		}

		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(20000), true);
		StatsWriter.writeHistogram(hist, "dist", "p", "/home/johannes/gsv/matrices/analysis/berlin.dist.txt");
	}

}
