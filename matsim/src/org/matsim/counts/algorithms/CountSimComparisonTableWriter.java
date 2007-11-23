/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonTableWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.counts.algorithms;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.matsim.counts.CountSimComparison;

/**
 * This class can write a List of CountSimComparison Objects to a simple table
 * @author dgrether
 *
 */
public class CountSimComparisonTableWriter extends CountSimComparisonWriter {
	/**
   * the separator used
   */
	private static final String SEPARATOR = "\t";

	/**
   * newline
   */
	private static final String NEWLINE = "\n";

	/**
   * the column headers of the table
   */
	private static final String[] COLUMNHEADERS = { "Link Id", "Hour",
			"MATSIM volumes", "Real volumes", "Relative Error"};

	/**
   * the formatter for numbers
   */
	private final NumberFormat numberFormat;


	/**
   *
   * @param countSimComparisons
	 * @param l the Locale used
   */
	public CountSimComparisonTableWriter(
			final List<CountSimComparison> countSimComparisons, final Locale l) {
		super(countSimComparisons);
		this.numberFormat = NumberFormat.getInstance(l);
	}

	/**
	 * Writes the CountSimComparison List of this class to the file given
	 * by the parameter.
	 * @param filename
	 */
	public void write(final String filename) {
		System.out.println("Writing CountsSimComparison to " + filename);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			for (int i = 0; i < COLUMNHEADERS.length; i++) {
				out.write(COLUMNHEADERS[i]);
				out.write(SEPARATOR);
			}
			out.write(NEWLINE);

			for (CountSimComparison csc : this.countComparisonFilter.getCountsForHour(this.timeFilter)) {
				out.write(csc.getId().toString());
				out.write(SEPARATOR);
				out.write(Integer.toString(csc.getHour()));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(csc.getSimulationValue()));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(csc.getCountValue()));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(csc.calculateRelativeError()));
				out.write(NEWLINE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ignored) {}
			}
		}
	}
}
