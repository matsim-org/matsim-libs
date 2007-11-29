/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.withinday.trafficmanagement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;


/**
 * @author dgrether
 *
 */
public class VDSSignSpreadSheetWriter {
	/**
   * the separator used
   */
	protected static final String SEPARATOR = "\t";

	/**
   * newline
   */
	protected static final String NEWLINE = "\n";
	/**
   * the column headers of the table
   */
	private static final String[] COLUMNHEADERS = { "System Time", "Measured Time Main Route",
			"Measured Time Alternative Route", "Nash Time"};
	/**
   * the formatter for numbers
   */
	private final NumberFormat numberFormat;

	private BufferedWriter bufferedWriter;


	public VDSSignSpreadSheetWriter(final BufferedWriter bufferedWriter) {
		this.numberFormat = NumberFormat.getInstance(Locale.getDefault());
		this.bufferedWriter = bufferedWriter;
	}


	public void writeHeader() throws IOException {
		for (int i = 0; i < COLUMNHEADERS.length; i++) {
			this.bufferedWriter.write(COLUMNHEADERS[i]);
			this.bufferedWriter.write(SEPARATOR);
		}
		this.bufferedWriter.write(NEWLINE);
	}

	public void writeLine(double time, double measuredTTMainRoute,
			double measuredTTAltRoute, double nashTime) throws IOException {
		this.bufferedWriter.write(this.numberFormat.format(time));
		this.bufferedWriter.write(SEPARATOR);
		this.bufferedWriter.write(this.numberFormat.format(measuredTTMainRoute));
		this.bufferedWriter.write(SEPARATOR);
		this.bufferedWriter.write(this.numberFormat.format(measuredTTAltRoute));
		this.bufferedWriter.write(SEPARATOR);
		this.bufferedWriter.write(this.numberFormat.format(nashTime));
		this.bufferedWriter.write(NEWLINE);
	}

	public void close() throws IOException {
		this.bufferedWriter.flush();
		this.bufferedWriter.close();
	}

}
