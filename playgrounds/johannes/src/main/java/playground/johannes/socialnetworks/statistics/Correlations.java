/* *********************************************************************** *
 * project: org.matsim.*
 * Correlations.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.statistics;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TDoubleIntHashMap;
import gnu.trove.TDoubleObjectHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.DummyDiscretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.utils.TXTWriter;

/**
 * @author illenberger
 * 
 */
public class Correlations {

	/**
	 * @param valuesX
	 *            a set of values
	 * @param valuesY
	 *            a set of values
	 * @return a map where the keys are the elements of <tt>valuesX</tt> and
	 *         where the values are the mean of a subset of <tt>valuesY</tt> the
	 *         index of which is equal to elements of <tt>valuesX</tt> with
	 *         equal value.
	 */
	public static TDoubleDoubleHashMap mean(double[] valuesX, double[] valuesY) {
		return mean(valuesX, valuesY, new DummyDiscretizer());
	}

	/**
	 * 
	 * @param valuesX
	 *            a set of values.
	 * @param valuesY
	 *            a set of values.
	 * @param binwidth
	 *            the bin width for a {@link LinearDiscretizer} applied to
	 *            elements of <tt>valuesX</tt>.
	 * @return a map where the keys are the elements of <tt>valuesX</tt> and
	 *         where the values are the mean of a subset of <tt>valuesY</tt> the
	 *         index of which is equal to elements of <tt>valuesX</tt> with
	 *         equal value. The elements of <tt>valuesX</tt> are discretized
	 *         with <tt>binwidth</tt>.
	 */
	public static TDoubleDoubleHashMap mean(double[] valuesX, double[] valuesY, double binwidth) {
		return mean(valuesX, valuesY, new LinearDiscretizer(binwidth));
	}

	/**
	 * 
	 * @param valuesX
	 *            a set of values.
	 * @param valuesY
	 *            a set of values.
	 * @param discretizer
	 *            a discretizer applied to the elements of <tt>valuesX</tt>.
	 * @return a map where the keys are the elements of <tt>valuesX</tt> and
	 *         where the values are the mean of a subset of <tt>valuesY</tt> the
	 *         index of which is equal to elements of <tt>valuesX</tt> with
	 *         equal value. The elements of <tt>valuesX</tt> are discretized
	 *         with <tt>discretizer</tt>.
	 */
	public static TDoubleDoubleHashMap mean(double[] valuesX, double[] valuesY, Discretizer discretizer) {
		if (valuesX.length != valuesY.length)
			throw new IllegalArgumentException("Both arrays must not differ in size!");

		TDoubleDoubleHashMap sums = new TDoubleDoubleHashMap();
		TDoubleIntHashMap counts = new TDoubleIntHashMap();

		for (int i = 0; i < valuesX.length; i++) {
			double key = valuesX[i];
			key = discretizer.discretize(key);
			sums.adjustOrPutValue(key, valuesY[i], valuesY[i]);
			counts.adjustOrPutValue(key, 1, 1);
		}

		TDoubleDoubleIterator it = sums.iterator();
		for (int i = 0; i < sums.size(); i++) {
			it.advance();
			it.setValue(it.value() / (double) counts.get(it.key()));
		}

		return sums;
	}

	/**
	 * 
	 * @param valuesX
	 *            a set of values.
	 * @param valuesY
	 *            a set of values.
	 * @param discretizer
	 *            a discretizer applied to the elements of <tt>valuesX</tt>.
	 * @return a map where the keys are the elements of <tt>valuesX</tt> and
	 *         where the values are descriptive statistics objects of a subset
	 *         of <tt>valuesY</tt> the index of which is equal to elements of
	 *         <tt>valuesX</tt> with equal value. The elements of
	 *         <tt>valuesX</tt> are discretized with <tt>discretizer</tt>.
	 */
	public static TDoubleObjectHashMap<DescriptiveStatistics> statistics(double[] valuesX, double[] valuesY,
			Discretizer discretizer) {
		if (valuesX.length != valuesY.length)
			throw new IllegalArgumentException("Both arrays must not differ in size!");

		TDoubleObjectHashMap<DescriptiveStatistics> map = new TDoubleObjectHashMap<DescriptiveStatistics>();

		for (int i = 0; i < valuesX.length; i++) {
			double x = discretizer.discretize(valuesX[i]);
			DescriptiveStatistics stats = map.get(x);
			if (stats == null) {
				stats = new DescriptiveStatistics();
				map.put(x, stats);
			}
			stats.addValue(valuesY[i]);
		}

		return map;
	}

	/**
	 * Writes a tab-separated file with the keys of <tt>values</tt> in the first
	 * column and the values of <tt>values</tt> in the second column.
	 * 
	 * @param values
	 *            a map of values.
	 * @param filename
	 *            the file name.
	 * @param xLabel
	 *            the header for the values of the first row.
	 * @param yLabel
	 *            the header for the values of the second row.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @deprecated
	 */
	public static void writeToFile(TDoubleDoubleHashMap values, String filename, String xLabel, String yLabel)
			throws FileNotFoundException, IOException {
		TXTWriter.writeMap(values, xLabel, yLabel, filename);
	}

}