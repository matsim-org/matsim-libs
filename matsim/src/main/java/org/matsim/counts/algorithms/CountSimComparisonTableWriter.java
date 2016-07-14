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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.CountSimComparison;

/**
 * This class can write a List of CountSimComparison Objects to a simple table
 * @author dgrether
 *
 * anhorni 31.03.2008: Writing the 'average weekday traffic volume' (german: DWV) per link
 * to another table.
 * Future work: Use parameter in the config file to choose which table to be printed.
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
			"MATSIM volumes", "Count volumes", "Relative Error"};

	/**
   * the formatter for numbers
   */
	private final NumberFormat numberFormat;

	private static final Logger log = Logger.getLogger(CountSimComparisonTableWriter.class);

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
			for (int i = 0; i < COLUMNHEADERS.length; i++) {
				out.write(COLUMNHEADERS[i]);
				out.write(SEPARATOR);
			}
			out.write(NEWLINE);

			for (CountSimComparison csc : this.countComparisonFilter.getCountsForHour(null)) {

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

		// writing the 'average weekday traffic volume' table:
		this.writeAWTVTable(filename);

	}

	private void writeAWTVTable(final String file) {
		// output file always ends with ".txt"
		String filename=file.substring(0,file.length()-4)+"AWTV.txt";

		log.info("Writing 'average weekday traffic volume' to " + filename);

		CountSimComparisonLinkFilter linkFilter=new CountSimComparisonLinkFilter(
				this.countComparisonFilter.getCountsForHour(null));

		try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
			out.write("Link Id\tMATSIM volumes\tCount volumes");
			out.write(NEWLINE);

			Iterator<Id<Link>> id_it = new CountSimComparisonLinkFilter(
					this.countComparisonFilter.getCountsForHour(null)).getLinkIds().iterator();

			while (id_it.hasNext()) {
				Id<Link> id= id_it.next();
				out.write(id.toString());
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(linkFilter.getAggregatedSimValue(id)));
				out.write(SEPARATOR);
				out.write(this.numberFormat.format(linkFilter.getAggregatedCountValue(id)));
				out.write(NEWLINE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
