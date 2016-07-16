/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusNetworkFix
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
package playground.dgrether.signalsystems.cottbus.scripts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;


/**
 * @author dgrether
 *
 */
public class DgCottbusNetworkFix {

	private static final Logger log = Logger.getLogger(DgCottbusNetworkFix.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String netFile = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_wgs84_utm33n.xml.gz";
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFile);

		BufferedReader linksReader = IOUtils.getBufferedReader("/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/links_no_lanes_changed.txt");
		String line = linksReader.readLine();
		while (line != null){
			log.info("Changing link id " + line);
			Id<Link> id = Id.create(line.trim(), Link.class);
			Link link = network.getLinks().get(id);
			link.setNumberOfLanes(2.0);
//			double capacity = link.getCapacity();
//			link.setCapacity(capacity * 2.0);
//			line = linksReader.readLine();
		}
		
		new NetworkWriter(network).write(netFile);
	}

}
