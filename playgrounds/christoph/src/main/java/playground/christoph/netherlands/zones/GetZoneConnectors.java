/* *********************************************************************** *
 * project: org.matsim.*
 * GetZoneConnectors.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.netherlands.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;

public class GetZoneConnectors {

	private static final Logger log = Logger.getLogger(GetZoneConnectors.class);
	
	private static String networkFile = "../../matsim/mysimulations/netherlands/network/network_with_connectors.xml.gz";
	
	private Map<Integer, List<Id>> mapping;	// TAZ, List<LinkId>
	
	public static void main(String[] args)  throws Exception {
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new GetZoneConnectors(scenario.getNetwork());
	}

	public GetZoneConnectors(Network network) throws Exception {
		log.info("Extracting connector links to zones mapping from network.");
		
		mapping = new HashMap<Integer, List<Id>>();

		for (Link link : network.getLinks().values()) {
			String idString = link.getId().toString();
			
			if (idString.endsWith("from")) {
				idString = idString.replace("from", "");
			} else if (idString.endsWith("to")) {
				idString = idString.replace("to", "");
			} else continue;
			
			int TAZ = Integer.valueOf(idString);
			
			List<Id> list = mapping.get(TAZ);
			if (list == null) {
				list = new ArrayList<Id>();
				mapping.put(TAZ, list);
			}
			list.add(link.getId());
		}
		log.info("Found " + mapping.size() + " mappings.");
	}
	
	public List<Id> getMappingForTAZ(int TAZ) {
		return mapping.get(TAZ);
	}
	
	public Map<Integer, List<Id>> getMapping() {
		return mapping;
	}
}
