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

package org.matsim.contrib.common.stats;

import gnu.trove.TDoubleArrayList;

import java.util.Arrays;

/**
 * @author johannes
 *
 */
public class InterpolatingDiscretizer implements Discretizer {

	private double[] binValues;
	
	private Discretizer borders;
	
	public InterpolatingDiscretizer(double[] values) {
		Arrays.sort(values);
		TDoubleArrayList tmpBorders = new TDoubleArrayList();
		TDoubleArrayList tmpValues = new TDoubleArrayList();
		double low = values[0];
		double high;
		for(int i = 1; i < values.length; i++) {
			high = values[i];
			if(low < high) {
				tmpBorders.add(low + (high - low)/2.0);
				tmpValues.add(low);
			}
			low = high;
		}
		tmpValues.add(values[values.length - 1]);
		
		borders = new FixedBordersDiscretizer(tmpBorders.toNativeArray());
		binValues = tmpValues.toNativeArray();
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.common.stats.Discretizer#discretize(double)
	 */
	@Override
	public double discretize(double value) {
		int idx = (int)index(value);
		return binValues[idx];
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.common.stats.Discretizer#index(double)
	 */
	@Override
	public int index(double value) {
		return borders.index(value);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.common.stats.Discretizer#binWidth(double)
	 */
	@Override
	public double binWidth(double value) {
		return borders.binWidth(value);
	}

}
