/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.counts;

import java.util.Iterator;
import java.util.List;

/**
 * This class provides methods to aggregate the errors
 * and the bias of a CountSimComparison instances list
 * to hourly based mean values.
 *
 * @author dgrether
 */
public class ComparisonErrorStatsCalculator {

	private double[] meanRelError = null;

	private double[] meanAbsError = null;

	private double[] meanAbsBias = null;

	private int[] nbrAbsBias = null;

	private int[] nbrRelErr = null;

	private int[] nbrAbsErr = null;
	
	public ComparisonErrorStatsCalculator(final List<CountSimComparison> ccl) {
		this.calculateErrorStats(ccl);
	}

	private void calculateErrorStats(final List<CountSimComparison> ccl) {
		// init with same value. Shorter stat.?
		// Possibly not all links have values for all hours.
		meanRelError = new double[24];
		nbrRelErr = new int[24];
		meanAbsError = new double[24];
		nbrAbsErr = new int[24];
		meanAbsBias = new double[24];
		nbrAbsBias = new int[24];
		Iterator<CountSimComparison> l_it = ccl.iterator();
		while (l_it.hasNext()) {
			CountSimComparison cc = l_it.next();
			int hour = cc.getHour() - 1;
			meanRelError[hour] += Math.abs(cc.calculateRelativeError());
			nbrRelErr[hour]++;

			meanAbsError[hour] += Math.abs(cc.getSimulationValue() - cc.getCountValue());
			nbrAbsErr[hour]++;

			meanAbsBias[hour] += cc.getSimulationValue() - cc.getCountValue();
			nbrAbsBias[hour]++;
		}// while

		for (int h = 0; h < 24; h++) {
			if (nbrRelErr[h] > 0) {
				meanRelError[h] /= nbrRelErr[h];
			} else {
				meanRelError[h] = 1000.0;
			}
			if (nbrAbsErr[h] > 0) {
				meanAbsError[h] /= nbrAbsErr[h];
			} else {
				meanAbsError[h] = 1.0;
			}
			if (nbrAbsBias[h] > 0) {
				meanAbsBias[h] /= nbrAbsBias[h];
			} else {
				meanAbsBias[h] = 1.0;
			}
		}
	}


	public double[] getMeanRelError() {
		if (meanRelError == null) {
			throw new RuntimeException("Object not initialized correctly. Call calculateErrorStats(..) first!");
		}
		return meanRelError.clone();
	}


	public double[] getMeanAbsError() {
		if (meanAbsError == null) {
			throw new RuntimeException("Object not initialized correctly. Call calculateErrorStats(..) first!");
		}
		return meanAbsError.clone();
	}


	public double[] getMeanAbsBias() {
		if (meanAbsBias == null) {
			throw new RuntimeException("Object not initialized correctly. Call calculateErrorStats(..) first!");
		}
		return meanAbsBias.clone();
	}


}
