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
package playground.agarwalamit.siouxFalls.simulationInputs;

import java.io.BufferedWriter;
import java.io.IOException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
/**
 * This was used when there were no way to identify the road category of old Sioux falls network (fewer links). 
 * @author amit
 *
 */
// ZZ_TODO : probably mode this to some templates.
public class AdditionOfRoadTypeInNetwork {
	
	private static BufferedWriter writer;
	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("./input/output_network.xml.gz");
		writer = IOUtils.getBufferedWriter("./input/baseCase/roadTypeMapping.txt");

		for (Link link : network.getLinks().values()) {
			
				double speedInMPS = link.getFreespeed();
				if (speedInMPS == 25.0) {
					NetworkUtils.setType(link, "01");
				} else if (speedInMPS == 13.9) {
					NetworkUtils.setType(link, "02");
				} else {
					throw new RuntimeException(
							"Define road type in roadTypeMapping file for this category."
									+ speedInMPS);
					
				}
				link.setCapacity(3*link.getCapacity());
				network.addLink(link); // This will give warning for duplicat of link in network which can be ignored as same link is added with type of linkImpl.
		}
		new NetworkWriter(network).write("./input/baseCase/output_networkWithRoadType.xml.gz");
		writeRoadTypeMappingFile();
	}

	public static void writeRoadTypeMappingFile() {
		try {
			writer.write("VISUM_RT_NR" + ";" + "VISUM_RT_NAME" + ";"
					+ "HBEFA_RT_NAME" + "\n");
			writer.write("01" + ";" + "Faster Link" + ";" + "URB/MW-City/90" + "\n");
			writer.write("02" + ";" + "Slower Link " + ";" + "URB/Distr/50"+ "\n");
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Counld not write file.", e);
		}
	}
}