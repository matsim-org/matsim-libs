/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonTableWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.counts.CountSimComparison;

/**
 * This class can write a List of CountSimComparison Objects to a simple table
 * @author dgrether
 *
 * anhorni 31.03.2008: Writing the 'average weekday traffic volume' (german: DWV) per link
 * to another table.
 * Future work: Use parameter in the config file to choose which table to be printed.
 * 
 * Added normalized relative errors and geh values to output file. cdobler jun'17
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
	private static final String[] COLUMNHEADERS = { "Link Id", "Count Station Id", "Hour",
			"MATSIM volumes", "Count volumes", "Relative Error", "Normalized Relative Error", "GEH"};

	/**
	 * the formatter for numbers
	 */
	private final NumberFormat numberFormat;

	private static final Logger log = LogManager.getLogger(CountSimComparisonTableWriter.class);

	/**
	 *
	 * @param countSimComparisons
	 * @param l the Locale used, if null then DecimalFormat("#.############") is used
	 */
	public CountSimComparisonTableWriter(
			final List<CountSimComparison> countSimComparisons, final Locale l) {
		super(countSimComparisons);
		if (l == null) {
			this.numberFormat = new DecimalFormat("#.############");
		}
		else {
			this.numberFormat = NumberFormat.getInstance(l);
		}
		this.numberFormat.setGroupingUsed(false);
	}

	/**
	 * Writes the CountSimComparison List of this class to the file given
	 * by the parameter.
	 * @param filename
	 */
	@Override
	public void writeFile(final String filename) {
		log.info("Writing CountsSimComparison to " + filename);
		try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
			
			out.write(COLUMNHEADERS[0]);
			for (int i = 1; i < COLUMNHEADERS.length; i++) {
				out.write(SEPARATOR);
				out.write(COLUMNHEADERS[i]);
			}
			out.write(NEWLINE);

			for (CountSimComparison csc : this.countComparisonFilter.getCountsForHour(null)) {
				
				out.write(csc.getId().toString());
				out.write(SEPARATOR);
				out.write(csc.getCsId());
				out.write(SEPARATOR);
				out.write(Integer.toString(csc.getHour()));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(csc.getSimulationValue()));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(csc.getCountValue()));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(csc.calculateRelativeError()));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(csc.calculateNormalizedRelativeError()));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(csc.calculateGEHValue()));
				out.write(NEWLINE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// writing the 'average weekday traffic volume' table:
		this.writeAWTVTable(filename);
	}

	private void writeAWTVTable(final String file) {
		// output file always ends with ".txt"
		String filename = file.substring(0, file.length() - 4) + "AWTV.txt";

		log.info("Writing 'average weekday traffic volume' to " + filename);

		CountSimComparisonLinkFilter linkFilter = new CountSimComparisonLinkFilter(
				this.countComparisonFilter.getCountsForHour(null));

		try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
			out.write("Link Id");
			out.write(SEPARATOR);
			out.write("Count Station Id");
			out.write(SEPARATOR);
			out.write("MATSIM volumes");
			out.write(SEPARATOR);
			out.write("Count volumes");
			out.write(SEPARATOR);
			out.write("Normalized Relative Error");
			out.write(NEWLINE);

			for (CountSimComparison csc : this.countComparisonFilter.getCountsForHour(null)) {

				final double simValue = linkFilter.getAggregatedSimValue(csc.getId());
				final double countValue = linkFilter.getAggregatedCountValue(csc.getId());
				final double nomRelError;
				
				final double max = Math.max(simValue, countValue);
				if (max == 0.0) nomRelError = 0;
				else nomRelError = Math.abs(simValue - countValue) / max;
				
				out.write(csc.getId().toString());
				out.write(SEPARATOR);
				out.write(csc.getCsId());
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(simValue));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(countValue));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(nomRelError));
				out.write(NEWLINE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}