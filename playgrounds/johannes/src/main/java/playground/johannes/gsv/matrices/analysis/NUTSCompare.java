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
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;
import playground.johannes.synpop.matrix.ODMatrixOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class NUTSCompare {

	private static final Logger logger = Logger.getLogger(NUTSCompare.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String itpFile = "/home/johannes/gsv/matrices/refmatrices/itp.xml";
		String tomtomFile  = "/home/johannes/gsv/matrices/refmatrices/tomtom.de.xml";
		String simFile = "/home/johannes/gsv/matrices/simmatrices/miv.744.xml";
		
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.gk3.geojson")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		data = null;
		zones.setPrimaryKey("gsvId");
		
		double distThreshold = 100000;
		/*
		 * itp
		 */
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(itpFile);
		NumericMatrix itpNuts3 = reader.getMatrix();
		
		MatrixOperations.applyFactor(itpNuts3, 1/365.0);
		removeEntries(itpNuts3, zones, distThreshold);
		/*
		 * tomtom
		 */
		reader.parse(tomtomFile);
		NumericMatrix tomtomNuts3 = reader.getMatrix();
		removeEntries(tomtomNuts3, zones, distThreshold);
		/*
		 * sim
		 */
		reader.parse(simFile);
		NumericMatrix simNuts3 = reader.getMatrix();
		
		MatrixOperations.applyFactor(simNuts3, 11);
		MatrixOperations.applyDiagonalFactor(simNuts3, 1.3);
		removeEntries(simNuts3, zones, distThreshold);
		
		double itpSum = MatrixOperations.sum(itpNuts3);
		double tomtomSum = MatrixOperations.sum(tomtomNuts3);
		double simSum = MatrixOperations.sum(simNuts3);
		
		MatrixOperations.applyFactor(itpNuts3, 1/itpSum);
		MatrixOperations.applyFactor(tomtomNuts3, 1/tomtomSum);
		MatrixOperations.applyFactor(simNuts3, 1/simSum);
		
		logger.info(String.format("Matrix sum: itp = %s, tomtom = %s, sim = %s", itpSum, tomtomSum, simSum));
		
		NumericMatrix itpNuts1 = ODMatrixOperations.aggregate(itpNuts3, zones, "nuts1_code");
		NumericMatrix tomtomNuts1 = ODMatrixOperations.aggregate(tomtomNuts3, zones, "nuts1_code");
		NumericMatrix simNuts1 = ODMatrixOperations.aggregate(simNuts3, zones, "nuts1_code");
		
		logger.info(String.format("DE1-DE9 itp: %s", itpNuts1.get("DE2", "DE9")));
		logger.info(String.format("DE1-DE9 tomtom: %s", tomtomNuts1.get("DE2", "DE9")));
		logger.info(String.format("DE1-DE9 sim: %s", simNuts1.get("DE2", "DE9")));
		
		DescriptiveStatistics stats = absError(tomtomNuts1, simNuts1);
		printStats(stats, "NUTS1 sim");
		stats = absError(tomtomNuts1, itpNuts1);
		printStats(stats, "NUTS1 itp");
		
//		
//		KeyMatrix itpNuts2 = MatrixOperations.aggregate(itpNuts3, zones, "nuts2_code");
//		KeyMatrix tomtomNuts2 = MatrixOperations.aggregate(tomtomNuts3, zones, "nuts2_code");
//		KeyMatrix simNuts2 = MatrixOperations.aggregate(simNuts3, zones, "nuts2_code");
//		
//		stats = absError(tomtomNuts2, simNuts2);
//		printStats(stats, "NUTS2 sim");
//		stats = absError(tomtomNuts2, itpNuts2);
//		printStats(stats, "NUTS2 itp");
//
//		stats = absError(tomtomNuts3, simNuts3);
//		printStats(stats, "NUTS3 sim");
//		stats = absError(tomtomNuts3, itpNuts3);
//		printStats(stats, "NUTS3 itp");
//		
//		addInhabCats(zones);
//		
//		KeyMatrix itpInhab = MatrixOperations.aggregate(itpNuts3, zones, "inhabCat");
//		KeyMatrix tomtomInhab = MatrixOperations.aggregate(tomtomNuts3, zones, "inhabCat");
//		KeyMatrix simInhab = MatrixOperations.aggregate(simNuts3, zones, "inhabCat");
//		
//		itpSum = MatrixOperations.sum(itpInhab);
//		tomtomSum = MatrixOperations.sum(tomtomInhab);
//		simSum = MatrixOperations.sum(simInhab);
//		
//		MatrixOperations.multiply(itpInhab, 1/itpSum);
//		MatrixOperations.multiply(tomtomInhab, 1/tomtomSum);
//		MatrixOperations.multiply(simInhab, 1/simSum);
		
		
//		DescriptiveStatistics stats = absError(tomtomInhab, itpInhab);
//		printStats(stats, "Inhabitants itp");
//		stats = absError(tomtomInhab, simInhab);
//		printStats(stats, "Inhabitants sim");
	}
	
	private static void printStats(DescriptiveStatistics stats, String name) {
		logger.info(String.format("%s : mean = %.2f, median = %.2f, var = %.2f, min = %.2f, max = %.2f", name, stats.getMean(), stats.getPercentile(50), stats.getVariance(), stats.getMin(), stats.getMax()));
	}
	
	private static DescriptiveStatistics absError(NumericMatrix m1, NumericMatrix m2) {
		NumericMatrix errMatrix = new NumericMatrix();
		MatrixOperations.errorMatrix(m1, m2, errMatrix);
//		KeyMatrix errMatrix = MatrixOperations.diffMatrix(m1, m2);
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		Set<String> keys = errMatrix.keys();
		
		for (String i : keys) {
			for (String j : keys) {
//				if(i != j) {
				Double val = errMatrix.get(i, j);
				if (val != null) {
					stats.addValue(Math.abs(val));
//					stats.addValue(val);
//					if(val >= 1)
					logger.info(String.format("%s - %s: %s", i, j, val));
//				}
				}
			}
		}
		
		return stats;
	}
	
	private static void removeEntries(NumericMatrix m, ZoneCollection zones, double distThreshold) {
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		Set<String> keys = m.keys();
		int cnt = 0;
		for (String i : keys) {
			for (String j : keys) {
				Zone zone_i = zones.get(i);
				Zone zone_j = zones.get(j);

				if (zone_i != null && zone_j != null) {
					Point pi = zone_i.getGeometry().getCentroid();
					Point pj = zone_j.getGeometry().getCentroid();

					double d = dCalc.distance(pi, pj);

					if (d < distThreshold) {
						Double val = m.get(i, j);
						if (val != null) {
							m.set(i, j, null);
							cnt++;
						}
					}
				}
			}
		}
		
		logger.info(String.format("Removed %s trips with less than %s KM.", cnt, distThreshold));
	}

	private static void addInhabCats(ZoneCollection zones) {
		Discretizer disc = new LinearDiscretizer(250000);
		for(Zone zone : zones.getZones()) {
			double val = Double.parseDouble(zone.getAttribute("inhabitants"));
			val = disc.index(val);
			val = Math.min(5, val);
			zone.setAttribute("inhabCat", String.valueOf(val));
		}
	}
}
