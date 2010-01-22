/* *********************************************************************** *
 * project: org.matsim.*
 * CapacityTest.java
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

package playground.yu.test;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

public class CapacityTest {

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch-osm-wu.xml";
		final String outputFilename = "test/yu/test/captest.txt";

		Scenario scenario = new ScenarioImpl();
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outputFilename);
			for (Link link : network.getLinks().values()) {
				out.write(link.getId()
								+ "\t"
								+ link.getCapacity(Time.UNDEFINED_TIME)
								+ "\n");
				out.flush();
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
