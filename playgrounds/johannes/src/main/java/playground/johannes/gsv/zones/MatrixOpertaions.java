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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

	public static void applyDiagonalFactor(KeyMatrix m, double factor) {
		Set<String> keys = m.keys();
		for (String i : keys) {
			Double val = m.get(i, i);
			if (val != null) {
				m.set(i, i, val * factor);
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
					// if(i == j) val = val/2.0;
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

	public static KeyMatrix average(Collection<KeyMatrix> matrices) {
		Set<String> keys = new HashSet<>();
		for (KeyMatrix m : matrices) {
			keys.addAll(m.keys());
		}

		KeyMatrix avr = new KeyMatrix();
		for (KeyMatrix m : matrices) {
			for (String i : keys) {
				for (String j : keys) {
					Double val = m.get(i, j);
					if (val == null)
						val = 0.0;

					Double val2 = avr.get(i, j);
					if (val2 == null)
						val2 = 0.0;

					double sum = val + val2;
					if (sum > 0)
						avr.set(i, j, sum);
				}
			}
		}

		for (String i : keys) {
			for (String j : keys) {
				avr.applyFactor(i, j, 1 / (double) matrices.size());
			}
		}

		return avr;
	}

	public static void sysmetrize(KeyMatrix m) {
		List<String> keys = new ArrayList<>(m.keys());

		for (int i = 0; i < keys.size(); i++) {
			String key1 = keys.get(i);
			for (int j = (i + 1); j < keys.size(); j++) {
				String key2 = keys.get(j);
				Double val1 = m.get(key1, key2);
				if (val1 == null)
					val1 = 0.0;

				Double val2 = m.get(key2, key1);
				if (val2 == null)
					val2 = 0.0;

				double sum = val1 + val2;
				// double p = Math.random() - 0.5;
				//
				// double f = 0.5 + (p * 0.1);
				//
				// double maxf = Math.max(f, 1-f);
				//
				// double v1;
				// double v2;
				//
				// if(val1 < val2) {
				// v1 = sum * maxf;
				// v2 = sum * (1 - maxf);
				// } else {
				// v1 = sum * (1 - maxf);
				// v2 = sum * maxf;
				// }

				m.set(key1, key2, sum / 2.0);
				m.set(key2, key1, sum / 2.0);
			}
		}
	}

	public static KeyMatrix aggregate(KeyMatrix m, Map<String, Zone> zones, String key) {
		KeyMatrix newM = new KeyMatrix();

		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = m.get(i, j);
				if (val != null) {
					Zone zone_i = zones.get(i);
					Zone zone_j = zones.get(j);

					String att_i = zone_i.getAttribute(key);
					String att_j = zone_j.getAttribute(key);

					newM.add(att_i, att_j, val);
				}
			}
		}
		
		return newM;
	}
}
