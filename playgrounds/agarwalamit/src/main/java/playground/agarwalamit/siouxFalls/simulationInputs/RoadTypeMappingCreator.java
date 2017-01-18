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
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
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
 * @author amit
 */
//ZZ_TODO : probably mode this to some templates.
public class RoadTypeMappingCreator {

	private static BufferedWriter writer;
	static SortedMap<String, String[] > roadTypeAndHBEFARoadType ;

	public static void main(String[] args) {

		roadTypeAndHBEFARoadType = new TreeMap<>();
		roadTypeAndHBEFARoadType.put("motorway", new String [] {"1","RUR/MW/120"});
		roadTypeAndHBEFARoadType.put("motorway_link", new String [] {"2","URB/MW-Nat./80"});
		roadTypeAndHBEFARoadType.put("trunk", new String [] {"3","URB/MW-City/80"});
		roadTypeAndHBEFARoadType.put("trunk_link", new String [] {"4","URB/Trunk-City/50"});
		roadTypeAndHBEFARoadType.put("primary", new String [] {"5","URB/Trunk-Nat./80"});
		roadTypeAndHBEFARoadType.put("primary_link", new String [] {"6","RUR/Distr-sin./60"});
		roadTypeAndHBEFARoadType.put("secondary", new String [] {"7","URB/Distr/60"});
		roadTypeAndHBEFARoadType.put("tertiary", new String [] {"8","URB/Local/50"});
		roadTypeAndHBEFARoadType.put("unclassified", new String [] {"9","URB/Access/50"});
		roadTypeAndHBEFARoadType.put("residential", new String [] {"10","URB/Access/30"}); 

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();

		new MatsimNetworkReader(scenario.getNetwork()).readFile("./input/baseCase/SiouxFalls_network_fromOSM.xml");
		writer = IOUtils.getBufferedWriter("./input/emissionFiles/SiouxFalls_roadTypeMapping.txt");

		for (Link link : network.getLinks().values()) {
			String linkIdentifier = link.getId().toString().split("___")[1];
			if(roadTypeAndHBEFARoadType.containsKey(linkIdentifier)){
				NetworkUtils.setType(link, roadTypeAndHBEFARoadType.get(linkIdentifier)[0]);
			} else {
				throw new RuntimeException("Road Category "+linkIdentifier+" is not defined.");
			}
			link.setCapacity(link.getCapacity());
			network.addLink(link); // This will give warning for duplicat of link in network which can be ignored as same link is added with type of linkImpl.
		}
		new NetworkWriter(network).write("./input/baseCase/SiouxFalls_networkWithRoadType.xml.gz");
		writeRoadTypeMappingFile();
	}

	private static void writeRoadTypeMappingFile() {
		try {
			writer.write("VISUM_RT_NR" + ";" + "VISUM_RT_NAME" + ";"+ "HBEFA_RT_NAME" + "\n");
			for(Entry<String, String[] > e: roadTypeAndHBEFARoadType.entrySet()) {
				writer.write(e.getValue()[0] + ";" + e.getKey() + ";" + e.getValue()[1] + "\n");
			} 
			writer.close();
		}catch (IOException e) {
			throw new RuntimeException("Counld not write file. Reason : ", e);
		}
	}
}
