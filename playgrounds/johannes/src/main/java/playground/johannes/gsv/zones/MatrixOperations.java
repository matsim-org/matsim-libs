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
import org.matsim.contrib.socnetgen.socialnetworks.utils.XORShiftRandom;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;

import java.util.*;

/**
 * @author johannes
 *
 */
public class MatrixOperations {

	public static KeyMatrix errorMatrix(KeyMatrix m1, KeyMatrix m2) {
		KeyMatrix mErr = new KeyMatrix();

		Set<String> zones = new HashSet<>(m1.keys());
		zones.addAll(m2.keys());

		for (String i : zones) {
			for (String j : zones) {
				Double val1 = m1.get(i, j);
				Double val2 = m2.get(i, j);

				if (val1 == null)
					val1 = new Double(0);

				if (val2 == null)
					val2 = new Double(0);

				if (val1 == 0 && val2 == 0)
					mErr.set(i, j, 0.0);
				else if (val1 == 0) {
					// do nothing
				} else {
//					if(val2 > 15) {
					Double err = (val2 - val1) / val1;
					mErr.set(i, j, err);
//					}
				}

			}
		}

		return mErr;
	}

	public static KeyMatrix diffMatrix(KeyMatrix m1, KeyMatrix m2) {
		KeyMatrix mErr = new KeyMatrix();

		Set<String> zones = new HashSet<>(m1.keys());
		zones.addAll(m2.keys());

		for (String i : zones) {
			for (String j : zones) {
				Double val1 = m1.get(i, j);
				Double val2 = m2.get(i, j);

				if (val1 == null)
					val1 = new Double(0);

				if (val2 == null)
					val2 = new Double(0);

				mErr.set(i, j, val1 - val2);
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

	public static double diagonalSum(KeyMatrix m) {
		double sum = 0;
		Set<String> keys = m.keys();
		for(String key : keys) {
			Double val = m.get(key, key);
			if(val != null) {
				sum += val;
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

//		for (String i : keys) {
//			for (String j : keys) {
//				avr.applyFactor(i, j, 1 / (double) matrices.size());
//			}
//		}

		applyFactor(avr, 1/(double)matrices.size());

		return avr;
	}

	public static void symetrize(KeyMatrix m) {
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
				if (sum > 0) {
					m.set(key1, key2, sum / 2.0);
					m.set(key2, key1, sum / 2.0);
				}
			}
		}
	}

	public static KeyMatrix aggregate(KeyMatrix m, ZoneCollection zones, String key) {
		KeyMatrix newM = new KeyMatrix();

		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = m.get(i, j);
				if (val != null) {
					Zone zone_i = zones.get(i);
					Zone zone_j = zones.get(j);

					if (zone_i != null && zone_j != null) {
						String att_i = zone_i.getAttribute(key);
						String att_j = zone_j.getAttribute(key);

						newM.add(att_i, att_j, val);
					}
				}
			}
		}

		return newM;
	}

	public static KeyMatrix merge(Collection<KeyMatrix> matrices) {
		KeyMatrix sum = new KeyMatrix();

		for(KeyMatrix m : matrices) {
			Set<String> keys = m.keys();
			for(String i : keys) {
				for(String j : keys) {
					Double val = m.get(i, j);
					if(val != null) {
						Double sumVal = sum.get(i, j);
						if(sumVal == null) sumVal = 0.0;

						sum.set(i, j, sumVal + val);
					}
				}
			}
		}

		return sum;
	}

	public static void randomize(KeyMatrix m, double fraction) {
		Set<String> keys = m.keys();
		Random random = new XORShiftRandom();
		for(String i : keys) {
			for(String j : keys) {
				Double val = m.get(i, j);
				if(val != null) {
					double f = (random.nextDouble() - 1) * 2 * fraction;
					double v = val + (val * f);
					m.set(i, j, v);
				}
			}
		}
	}

	public static double weightedCellAverage(KeyMatrix m, KeyMatrix weights) {
		double sum = 0;
		double wsum = 0;

		Set<String> keys = m.keys();
		for(String i : keys) {
			for(String j : keys) {
				Double val = m.get(i, j);
				if(val != null) {
					Double w = weights.get(i, j);
					if(w == null) w = 1.0;
					sum += val * w;
					wsum += w;
				}
			}
		}

		return sum/wsum;
	}

	public static void removeDiagonal(KeyMatrix m) {
		Set<String> keys = m.keys();
		for(String i : keys) {
			m.set(i, i, null);
		}
	}

	public static void add(KeyMatrix m1, KeyMatrix m2) {
		Set<String> keys = m2.keys();
		for(String i : keys) {
			for(String j : keys) {
				Double val2 = m2.get(i, j);
				if(val2 != null) {
					Double val1 = m1.get(i, j);
					if(val1 == null) val1 = 0.0;
					m1.set(i, j, val1 + val2);
				}
			}
		}
	}
}
