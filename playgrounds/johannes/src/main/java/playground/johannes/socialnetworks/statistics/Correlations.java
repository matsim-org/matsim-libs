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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class Correlations {

	/**
	 * @param values1
	 *            a set of values
	 * @param values2
	 *            a set of values
	 * @return a map where the keys are the elements of <tt>values1</tt> and
	 *         where the values are the mean of elements in a set L. L is the
	 *         set of elements of <tt>values2</tt> where the elements of
	 *         <tt>values1</tt> with same index are equal.
	 */
	public static TDoubleDoubleHashMap correlationMean(double[] values1, double[] values2) {
		return correlationMean(values1, values2, 1.0);
	}
	
	/**
	 * 
	 * @param values1
	 *            a set of values.
	 * @param values2
	 *            a set of values.
	 * @param binsize
	 *            a discretization constant of the elements in <tt>values1</tt>.
	 * @return a map where the keys are the elements of <tt>values1</tt> and
	 *         where the values are the mean of elements in a set L. L is the
	 *         set of elements of <tt>values2</tt> where the elements of
	 *         <tt>values1</tt> with same index are equal. The elements of
	 *         <tt>values1</tt> are discretized with <tt>binsize</tt>.
	 */
	public static TDoubleDoubleHashMap correlationMean(double[] values1, double[] values2, double binsize) {
		return correlationMean(values1, values2, new LinearDiscretizer(binsize));
	}
	
	public static TDoubleDoubleHashMap correlationMean(double[] values1, double[] values2, Discretizer discretizer) {
		if(values1.length != values2.length)
			throw new IllegalArgumentException("Both arrays must not differ in size!");
		
		TDoubleDoubleHashMap sums = new TDoubleDoubleHashMap();
		TDoubleIntHashMap counts = new TDoubleIntHashMap();
		
		for(int i = 0; i < values1.length; i++) {
			double key = values1[i];
//			if(binsize > 0.0)
//				key = Math.floor(key/binsize) * binsize;
			key = discretizer.discretize(key);
			sums.adjustOrPutValue(key, values2[i], values2[i]);
			counts.adjustOrPutValue(key, 1, 1);
		}
		
		TDoubleDoubleIterator it = sums.iterator();
		for(int i = 0; i < sums.size(); i++) {
			it.advance();
			it.setValue(it.value()/(double)counts.get(it.key()));
		}
		
		return sums;
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
	 */
	public static void writeToFile(TDoubleDoubleHashMap values, String filename, String xLabel, String yLabel) throws FileNotFoundException, IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write(xLabel);
		writer.write("\t");
		writer.write(yLabel);
		writer.newLine();
		double[] keys = values.keys();
		Arrays.sort(keys);
		for(double key : keys) {
			writer.write(String.valueOf(key));
			writer.write("\t");
			writer.write(String.valueOf(values.get(key)));
			writer.newLine();
		}
		writer.close();
	}
}