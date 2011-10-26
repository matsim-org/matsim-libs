/* *********************************************************************** *
 * project: org.matsim.*
 * NoiseTool.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.fhuelsmann.noise;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class Calculation {

	/* algorithm to calculate noise emissions */

	public Map<Id, Map<String, Double>> Cal(
			Map<Id, Map<String, double[]>> linkInfo) {

		System.out.println("Berechnung*--------------------*");
		Map<Id, Map<String, Double>> linkId2Time2NoiseEmissions = new TreeMap<Id, Map<String, Double>>();

		for (Entry<Id, Map<String, double[]>> entry : linkInfo.entrySet()) {
			Id linkId = entry.getKey();
			Map<String, double[]> time2TrafficInfo = entry.getValue();

			Map<String, Double> time2NoiseEmissions = new TreeMap<String, Double>();

			for (Entry<String, double[]> element : time2TrafficInfo.entrySet()) {
				String time = element.getKey();
				double[] trafficInfo = element.getValue();
				double maxSpeed = trafficInfo[0];
				/* from 10% to 100% sample */
				double dtv = 10.0 * trafficInfo[1];
				double heavy = 10.0 * trafficInfo[2];
				/* share of heavy duty traffic in total traffic */
				double shareHeavyInTotTraffic = heavy / dtv;

				double l_pkw = 27.7 + (10 * Math.log10(1.0 + Math.pow(
						0.02 * maxSpeed, 3.0)));
				double l_lkw = 23.1 + (12.5 * Math.log10(maxSpeed));
				double D = l_lkw - l_pkw;
				/* correction for max.speed */
				double Dv = l_pkw
						- 37.3
						+ 10
						* Math.log10((100.0 + (D / Math.pow(10.0, 10.0))
								* shareHeavyInTotTraffic)
								/ (100.0 + 8.23 * shareHeavyInTotTraffic));
				/* sourcelevel, Emissionspegel */
				double lm = calc_lm(time, dtv, shareHeavyInTotTraffic);
				/* Mittelungspegel */
				double lme = lm + Dv;
				time2NoiseEmissions.put(time, lme);
			}
			linkId2Time2NoiseEmissions.put(linkId, time2NoiseEmissions);
		}
		return linkId2Time2NoiseEmissions;
	}

	public double calc_lm(String periode, double dtv, double p) {
		double lm = 0.0;
		if (periode.equals("Tag")) {
			lm = 37.3 + 10.0 * (Math.log10(0.062 * dtv * (1.0 + 0.082 * p)));
		}
		if (periode.equals("Abend")) {
			lm = 37.3 + 10.0 * (Math.log10(0.042 * dtv * (1.0 + 0.082 * p)));
		}
		if (periode.equals("Nacht")) {
			lm = 37.3 + 10.0 * (Math.log10(0.011 * dtv * (1.0 + 0.082 * p)));
		}
		return lm;
	}

}
