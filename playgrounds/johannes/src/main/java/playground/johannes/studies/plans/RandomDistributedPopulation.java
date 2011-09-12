/* *********************************************************************** *
 * project: org.matsim.*
 * RandomDistribution.java
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
package playground.johannes.studies.plans;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * @author illenberger
 *
 */
public class RandomDistributedPopulation {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Random random = new Random(4711);
		int count = 1000;
		int scale = 200000;
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/work/socialnets/data/random/randompop.1000.xml"));
		writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		writer.newLine();
		writer.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">");
		writer.newLine();
		writer.write("<plans>");
		writer.newLine();
		
		for(int i = 0; i < count; i++) {
			writer.write("<person id=\"");
			writer.write(String.valueOf(i));
			writer.write("\">");
			writer.newLine();
			
			writer.write("<plan selected=\"yes\">");
			writer.newLine();
			
			writer.write("<act type=\"h\" x=\"");
			writer.write(String.valueOf(random.nextDouble() * scale));
			writer.write("\" y=\"");
			writer.write(String.valueOf(random.nextDouble() * scale));
			writer.write("\" />");
			writer.newLine();
			
			writer.write("</plan>");
			writer.newLine();
			
			writer.write("</person>");
			writer.newLine();
		}
		
		writer.write("</plans>");
		writer.close();
	}

}
