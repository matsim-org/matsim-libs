/* *********************************************************************** *
 * project: org.matsim.*
 * VolvoAnalysisWriter.java
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

package playground.dgrether.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Locale;



/**
 * @author dgrether
 *
 */
public class VolvoAnalysisWriter {
	/**
	 * the seperator used
	 */
	private static final String SEPERATOR = "\t";
	/**
	 * newline
	 */
	private static final String NEWLINE = "\n";
	/**
	 * the line headers of the table
	 */
	private static final String [] LINEHEADERS = {"Anzahl an Fahrten", "Distanz", "Fahrzeit"};
	/**
	 * the column headers of the table
	 */
	private static final String [] COLUMNHEADERS = {"", "Zeit", "Berlin Gesamt", "Berlin Hundekopf", "Berlin Rest"};
	/**
	 * the data
	 */
	private VolvoAnalysis volvoAnalysis;
	/**
	 * the formater for numbers
	 */
	private NumberFormat numberFormat;
	/**
	 * the current line
	 */
	private int line = 0;

	
	public VolvoAnalysisWriter(VolvoAnalysis analysis, Locale locale) {
		this.volvoAnalysis = analysis;
		numberFormat = NumberFormat.getInstance(locale);;
	}

	private void newline(Writer w, boolean newHeader) throws IOException {
    if (newHeader) {
    	line++;
    }
		w.write(NEWLINE);
		w.write(LINEHEADERS[line]);
    w.write(SEPERATOR);
	}
	
	/**
	 * Writes the analysis to a table
	 * @param outfilename
	 */
	public void write(String outfilename) {
		System.out.println("Writing VolvoAnalysis to " + outfilename);
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(outfilename));
//			out.write(COLUMNHEADERS[0]);
			for (int i = 0; i < COLUMNHEADERS.length; i++) {
				out.write(COLUMNHEADERS[i]);
				out.write(SEPERATOR);
			}
			//		write number of rides
			for (int i = 0; i < VolvoAnalysis.TIMESTEPS; i++) {
				newline(out, false);
				//				out.write(SEPERATOR);
				out.write(Integer.toString(i));
				out.write(SEPERATOR);
				out.write(Integer.toString(volvoAnalysis.getTripsGemarkung(i)));
				out.write(SEPERATOR);
				out.write(Integer.toString(volvoAnalysis.getTripsHundekopf(i)));
				out.write(SEPERATOR);
				out.write(Integer.toString(volvoAnalysis.getTripsRest(i)));
			}
			line++;
			// write distance
			for (int i = 0; i < VolvoAnalysis.TIMESTEPS; i++) {
				newline(out, false);
				out.write(Integer.toString(i));
				out.write(SEPERATOR);
				out.write(numberFormat.format(volvoAnalysis.getDistGemarkung(i)));
				out.write(SEPERATOR);
				out.write(numberFormat.format(volvoAnalysis.getDistHundekopf(i)));
				out.write(SEPERATOR);
				out.write(numberFormat.format(volvoAnalysis.getDistRest(i)));
			}
			line++;
			// write time
			for (int i = 0; i < VolvoAnalysis.TIMESTEPS; i++) {
				newline(out, false);
				out.write(Integer.toString(i));
				out.write(SEPERATOR);
				out.write(numberFormat.format(volvoAnalysis.getTimeGemarkung(i)));
				out.write(SEPERATOR);
				out.write(numberFormat.format(volvoAnalysis.getTimeHundekopf(i)));
				out.write(SEPERATOR);
				out.write(numberFormat.format(volvoAnalysis.getTimeRest(i)));
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end write
}
