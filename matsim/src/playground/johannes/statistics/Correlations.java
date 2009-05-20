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
package playground.johannes.statistics;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.core.utils.io.IOUtils;

import gnu.trove.TDoubleDoubleHashMap;

/**
 * @author illenberger
 *
 */
public class Correlations {

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
