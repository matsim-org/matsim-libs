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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author pboesch
 *
 */
public class ReducedLinkCapacityTemporaryArea {

	private static final Coord center = new CoordImpl(682952.0,247797.0); // A bit south of HB Zurich...)
	private static int radius;

	public static void main(String[] args) {
		String path2MATSimNetwork = args[0];
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(path2MATSimNetwork);
		Network network = scenario.getNetwork();
		String speedFactor = args[1];
		String capacityFactor = args[2];
		radius = Integer.parseInt(args[3]);

		final BufferedWriter out = IOUtils.getBufferedWriter(args[4]);
		try {
			// Header
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.newLine();
			out.write("<!DOCTYPE config SYSTEM \"http://www.matsim.org/files/dtd/networkChangeEvents.xsd\">");
			out.newLine();
			out.write("<networkChangeEvents>");
			out.newLine();
			// reduce capacity and speed:
			out.write("    <networkChangeEvent startTime=\"07:00:00\">"); out.newLine();
			for (Link link : network.getLinks().values()) {
				if (isLinkAffected(link)) {
					out.write("        <link refId=\"" + link.getId().toString() + "\"/>");
					out.newLine();
				}
			}
			out.write("        <flowCapacity type=\"scaleFactor\" value=\"" + capacityFactor + "\"/>");
			out.newLine();
			out.write("        <freespeed type=\"scaleFactor\" value=\"" + speedFactor + "\"/>");
			out.newLine();
			out.write("    </networkChangeEvent>");
			out.newLine();
			// reset capacity and speed:
			out.write("    <networkChangeEvent startTime=\"09:00:00\">"); out.newLine();
			for (Link link : network.getLinks().values()) {
				if (isLinkAffected(link)) {
					out.write("        <link refId=\"" + link.getId().toString() + "\"/>");
					out.newLine();
				}
			}
			out.write("        <flowCapacity type=\"scaleFactor\" value=\"1.0\"/>");
			out.newLine();
			out.write("        <freespeed type=\"scaleFactor\" value=\"1.0\"/>");
			out.newLine();
			out.write("    </networkChangeEvent>");
			out.newLine();
			// Footer:
			out.write("</networkChangeEvents>");
			out.newLine();

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean isLinkAffected(Link link) {
		return CoordUtils.calcDistance(link.getFromNode().getCoord(), center) <= radius ||
				CoordUtils.calcDistance(link.getToNode().getCoord(), center) <= radius ||
				CoordUtils.calcDistance(link.getCoord(), center) <= radius;
	}

}
