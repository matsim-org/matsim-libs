/* *********************************************************************** *
 * project: org.matsim.*
 * PlansGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.johannes.itsc08;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

public class PlansGenerator {

	public static void main(String args[]) throws FileNotFoundException, IOException {
		double starttime = 6*60*60 - 500;
		int numPersons = 1000;
		double numPersonsPerSecond = 2;
		
		final String saferoute = "2 4 5";
		final String riskyroute = "2 3 5";
		final String returnroute = "6 1";
		
		BufferedWriter writer = IOUtils.getBufferedWriter("/Users/fearonni/vsp-work/eut/2-routes/data/plans.xml");
		writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		writer.newLine();
		writer.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">");
		writer.newLine();
		writer.write("<plans>");
		writer.newLine();
		for(int i = 0; i < numPersons; i++) {
			writer.write("\t<person id=\"" + i + "\">");
			writer.newLine();
			/*
			 * safe plan
			 */
			writer.write("\t\t<plan selected=\"yes\">");
			writer.newLine();
			writer.write("\t\t\t<act type=\"h\" link=\"1\" end_time=\"" + Time.writeTime(starttime + (i/numPersonsPerSecond)) + "\"/>");
			writer.newLine();
			writer.write("\t\t\t<leg mode=\"car\"><route>");
			writer.write(saferoute);
			writer.write("</route></leg>");
			writer.newLine();
			writer.write("\t\t\t<act type=\"w\" link=\"6\" dur=\"08:00:00\"/>");
			writer.newLine();
			writer.write("\t\t\t<leg mode=\"car\"><route>");
			writer.write(returnroute);
			writer.write("</route></leg>");
			writer.newLine();
			writer.write("\t\t\t<act type=\"h\" link=\"1\" end_time=\"24:00:00\"/>");
			writer.newLine();
			writer.write("\t\t</plan>");
			writer.newLine();
			/*
			 * risky plan
			 */
			writer.write("\t\t<plan selected=\"no\">");
			writer.newLine();
			writer.write("\t\t\t<act type=\"h\" link=\"1\" end_time=\"" + Time.writeTime(starttime + (i/numPersonsPerSecond)) + "\"/>");
			writer.newLine();
			writer.write("\t\t\t<leg mode=\"car\"><route>");
			writer.write(riskyroute);
			writer.write("</route></leg>");
			writer.newLine();
			writer.write("\t\t\t<act type=\"w\" link=\"6\" dur=\"08:00:00\"/>");
			writer.newLine();
			writer.write("\t\t\t<leg mode=\"car\"><route>");
			writer.write(returnroute);
			writer.write("</route></leg>");
			writer.newLine();
			writer.write("\t\t\t<act type=\"h\" link=\"1\" end_time=\"24:00:00\"/>");
			writer.newLine();
			writer.write("\t\t</plan>");
			writer.newLine();
			
			writer.write("\t</person>");
			writer.newLine();
		}
		writer.write("</plans>");
		writer.close();
	}
}
