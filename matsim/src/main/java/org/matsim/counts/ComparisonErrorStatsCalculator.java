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

	private double[] meanBias = null;

	private int[] nbrBias = null;

	private int[] nbrRelErr = null;

	private int[] nbrAbsErr = null;
	
	public ComparisonErrorStatsCalculator(final List<CountSimComparison> ccl) {
		this.calculateErrorStats(ccl);
	}

	private void calculateErrorStats(final List<CountSimComparison> ccl) {
		// init with same value. Shorter stat.?
		// Possibly not all links have values for all hours.
		this.meanRelError = new double[24];
		this.nbrRelErr = new int[24];
		this.meanAbsError = new double[24];
		this.nbrAbsErr = new int[24];
		this.meanBias = new double[24];
		this.nbrBias = new int[24];
		Iterator<CountSimComparison> l_it = ccl.iterator();
		while (l_it.hasNext()) {
			CountSimComparison cc = l_it.next();
			int hour = cc.getHour() - 1;
			this.meanRelError[hour] += Math.abs(cc.calculateRelativeError());
			this.nbrRelErr[hour]++;

			this.meanAbsError[hour] += Math.abs(cc.getSimulationValue() - cc.getCountValue());
			this.nbrAbsErr[hour]++;

			this.meanBias[hour] += cc.getSimulationValue() - cc.getCountValue();
			this.nbrBias[hour]++;
		}// while

		for (int h = 0; h < 24; h++) {
			if (this.nbrRelErr[h] > 0) {
				this.meanRelError[h] /= this.nbrRelErr[h];
			} else {
				this.meanRelError[h] = 1000.0;
			}
			if (this.nbrAbsErr[h] > 0) {
				this.meanAbsError[h] /= this.nbrAbsErr[h];
			} else {
				this.meanAbsError[h] = 1.0;
			}
			if (this.nbrBias[h] > 0) {
				this.meanBias[h] /= this.nbrBias[h];
			} else {
				this.meanBias[h] = 1.0;
			}
		}
	}

	public double[] getMeanRelError() {
		if (this.meanRelError == null) {
			throw new RuntimeException("Object not initialized correctly. Call calculateErrorStats(..) first!");
		}
		return this.meanRelError.clone();
	}

	public double[] getMeanAbsError() {
		if (this.meanAbsError == null) {
			throw new RuntimeException("Object not initialized correctly. Call calculateErrorStats(..) first!");
		}
		return this.meanAbsError.clone();
	}

	public double[] getMeanBias() {
		if (this.meanBias == null) {
			throw new RuntimeException("Object not initialized correctly. Call calculateErrorStats(..) first!");
		}
		return this.meanBias.clone();
	}
}