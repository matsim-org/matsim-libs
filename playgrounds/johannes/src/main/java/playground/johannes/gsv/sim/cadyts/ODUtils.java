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

package playground.johannes.gsv.sim.cadyts;

import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;

import java.util.Set;

/**
 * @author johannes
 *
 */
public class ODUtils {

	private final static Logger logger = Logger.getLogger(ODUtils.class);
	
	public static void cleanDistances(KeyMatrix m, ZoneCollection zones, double distThreshold, DistanceCalculator dCalc) {
		/*
		 * remove entries below distance threshold
		 */
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

		logger.info(String.format("Removed %s trips with less than %s KM.", cnt, distThreshold / 1000.0));

	}
	
	public static void cleanDistances(KeyMatrix m, ZoneCollection zones, double distThreshold) {
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		cleanDistances(m, zones, distThreshold, dCalc);
	}
	
	public static void cleanVolumes(KeyMatrix m, ZoneCollection zones, double countThreshold) {
		/*
		 * remove reference relations below count threshold
		 */
		Set<String> keys = m.keys();
		int cnt = 0;
		for (String i : keys) {
			for (String j : keys) {
				Double val = m.get(i, j);
				if(val != null && val < countThreshold) {
					m.set(i, j, null);
					cnt++;
				}
			}
		}
		logger.info(String.format("Removed %s relations with less than %s trips.", cnt, countThreshold));
	}
	
	public static double calcNormalization(KeyMatrix m1, KeyMatrix m2) {
		Set<String> keys = m1.keys();
		
		double sum1 = 0;
		double sum2 = 0;
		
		for(String i : keys) {
			for(String j : keys) {
				Double val1 = m1.get(i, j);
				if(val1 != null) {
					Double val2 = m2.get(i, j);
					if(val2 == null) val2 = 0.0;
					
					sum1 += val1;
					sum2 += val2;
				}
			}
		}
		
		double c = sum2/sum1;
		logger.info(String.format("Trip sum matrix 1 = %s, matrix 2 = %s, factor = %s", sum1, sum2, c));
		
		return c;
	}
}
