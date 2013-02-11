/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.csestimation.biogeme;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;


public class ModFileWriter {

	
	private final static Logger log = Logger.getLogger(ModFileWriter.class);
	/**
	 * @param args	output file
	 */
	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		String outputFile = args[0];
		ModFileWriter writer = new ModFileWriter();
		writer.writeModFile(outputFile);
	}
	
	
	public void writeModFile(String outputFile) {
		String [] variables = {"addDist", "Size", "Price"};
		int numberOfAlternatives = 297;
		
		String openingBlock="[Choice]\n" +
			"Choice\n\n" +
			"//MODEL DESCRIPTION\n" +
			"//" + "shopping trip estimation" + "\n" + 
			"\n" +
			"[Beta]\n" +
			"//Name\tValue\tLower Bound\tUpperBound\tstatus (0=variable, 1=fixed)\n";
	
		for (int i = 0; i < variables.length; i++) {
			openingBlock += "B_" + variables[i] + "\t0\t-10.0\t10.0\t0\n";
		}	
		openingBlock += "\n";
		
		openingBlock += "[Utilities]\n" +
			"//Id\tName\tAvail\tlinear-in-parameter expression (beta1*x1 + beta2*x2 + ...)\n";
				
		String closingBlock = "[Expressions]\n" +
			"one = 1\n" +
			"\n" +
			"[Model]\n" +
			"$MNL  // Multinomial Logit Model\n";
		
		try {					
			final BufferedWriter out = IOUtils.getBufferedWriter(outputFile);
			out.write(openingBlock);
			
			for (int j = 0; j < numberOfAlternatives; j++) {
				String line = j + "\t" + "SH" + j + "\t" + "SH" + j + "_AV\t";
				
				for (int i = 0; i < variables.length; i++) {
					if (i > 0) {
						line += " + ";
					}
					line += "B_" + variables[i] + " * SH" + j + "_" + variables[i];
				}
				out.write(line);
				out.newLine();
				out.flush();
			}		
			out.newLine(); out.newLine();			
			out.write(closingBlock);
			out.flush();				
			out.close();
			
			log.info("Output file writen to :" + outputFile);
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}		
	}
}
