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

package playground.johannes.gsv.zones;

import gnu.trove.TObjectDoubleHashMap;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class MatrixOpertaions {

	public static KeyMatrix errorMatrix(KeyMatrix m1, KeyMatrix m2) {
		KeyMatrix mErr = new KeyMatrix();

		Set<String> zones = new HashSet<>(m1.keys());
		zones.addAll(m2.keys());

		for (String i : zones) {
			for (String j : zones) {
				Double val1 = m1.get(i, j);
				Double val2 = m2.get(i, j);

				if (!(val1 == null && val2 == null)) {
					if (val1 == null)
						val1 = new Double(0);
					if (val2 == null)
						val2 = new Double(0);

					Double err = (val2 - val1) / val1;

					if (val1 == 0 && val2 == 0)
						err = new Double(0);

					if (Double.isNaN(err)) {
						System.out.println();
					}
					mErr.set(i, j, err);
				}
			}
		}

		return mErr;
	}

	public static void applyFactor(KeyMatrix m, double factor) {
		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = m.get(i, j);
				if (val != null) {
					m.set(i, j, val * factor);
				}
			}
		}
	}

	public static double sum(KeyMatrix m) {
		double sum = 0;
		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = m.get(i, j);
				if (val != null) {
//					if(i == j) val = val/2.0;
					sum += val;
				}
			}
		}

		return sum;
	}

	public static TObjectDoubleHashMap<String> marginalsRow(KeyMatrix m) {
		TObjectDoubleHashMap<String> marginals = new TObjectDoubleHashMap<>();
		Set<String> keys = m.keys();
		for (String i : keys) {
			double sum = 0;
			for (String j : keys) {
				Double val = m.get(i, j);
				if (val != null) {
					sum += val;
				}
			}
			marginals.put(i, sum);
		}
		return marginals;
	}
	
	public static TObjectDoubleHashMap<String> marginalsCol(KeyMatrix m) {
		TObjectDoubleHashMap<String> marginals = new TObjectDoubleHashMap<>();
		Set<String> keys = m.keys();
		for (String j : keys) {
			double sum = 0;
			for (String i : keys) {
				Double val = m.get(i, j);
				if (val != null) {
					sum += val;
				}
			}
			marginals.put(j, sum);
		}
		return marginals;
	}
}
