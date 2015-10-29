/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.boescpa.topdad.subscenario;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author pboesch
 *
 */
public class ReducedLinkCapacityTemporary {
	
	public static void main(String[] args) {
		String path2MATSimNetwork = args[0];
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(path2MATSimNetwork);
		Network network = scenario.getNetwork();
		String speedFactor = args[1];
		String capacityFactor = args[2];

		final BufferedWriter out = IOUtils.getBufferedWriter(args[3]);
		try {
			// Header
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.newLine();
			out.write("<networkChangeEvents xmlns=\"http://www.matsim.org/files/dtd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/networkChangeEvents.xsd\">");
			out.newLine();
			// reduce capacity and speed:
			out.write("    <networkChangeEvent startTime=\"17:00:00\">");
			out.newLine();
			for (Link link : network.getLinks().values()) {
				out.write("        <link refId=\"" + link.getId().toString() + "\"/>");
				out.newLine();
			}
			out.write("        <flowCapacity type=\"scaleFactor\" value=\"" + capacityFactor + "\"/>");
			out.newLine();
			out.write("        <freespeed type=\"scaleFactor\" value=\"" + speedFactor + "\"/>");
			out.newLine();
			out.write("    </networkChangeEvent>");
			out.newLine();
			// reset capacity and speed:
			/*for (Link link : network.getLinks().values()) {
				out.write("    <networkChangeEvent startTime=\"09:00:00\">");
				out.newLine();
				out.write("        <link refId=\"" + link.getId().toString() + "\"/>");
				out.newLine();
				out.write("        <flowCapacity type=\"absolute\" value=\"" + link.getCapacity() + "\"/>");
				out.newLine();
				out.write("        <freespeed type=\"absolute\" value=\"" + link.getFreespeed() + "\"/>");
				out.newLine();
				out.write("    </networkChangeEvent>");
				out.newLine();
			}*/
			// Footer:
			out.write("</networkChangeEvents>");
			out.newLine();

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
