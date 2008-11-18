/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * @author dgrether
 *
 */
public class DoubleArrayTableWriter {
	
	private static final Logger log = Logger.getLogger(DoubleArrayTableWriter.class);
	
	/**
   * the separator used
   */
	private static final String SEPARATOR = "\t";

	/**
   * newline
   */
	private static final String NEWLINE = "\n";
	
	private List columns;
	
	public DoubleArrayTableWriter() {
		this.columns = new ArrayList();
	}
	
	
	public void addColumn(double[] columnData) {
		this.columns.add(columnData);
	}
	
	/**
	 * Writes the CountSimComparison List of this class to the file given
	 * by the parameter.
	 * @param filename
	 */
	public void writeFile(final String filename) {
		log.info("Writing table to " + filename);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(filename));
	
			int maxLength = 0;
			for (Object o : this.columns) {
				double[] a = (double[]) o;
				if (maxLength < a.length){
					maxLength = a.length;
				}
			}
			
			for (int i = 0; i < maxLength; i++) {
				for (Object o : this.columns) {
					double[] a = (double[]) o;
					if (i < a.length) {
						out.write(Double.toString(a[i]));
					}
					out.write(SEPARATOR);
				}
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
