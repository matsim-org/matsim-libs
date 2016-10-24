/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Remove certain links on the network so that the traffic along Gandhi setu disappears
 * @author amit
 */

public final class PatnaTrafficRestrainer {

	public static void main(String[] args) {
		String networkFile = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/network.xml.gz";
		Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		
		String filename = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/networkWithTrafficRestrication.xml.gz";

		PatnaTrafficRestrainer.run(scenario.getNetwork());
		new NetworkWriter(scenario.getNetwork()).write(filename);
	}

	PatnaTrafficRestrainer() {}

	public static void run(Network network){
		{
			// major problem with the follwoing links
			Id<Link> linkId = Id.createLinkId("1538010000");
			Id<Link> reverseLinkId = Id.createLinkId("15380");
			network.removeLink(linkId);
			network.removeLink(reverseLinkId);
		}

		{ 
			// does not look a major problem but the whole argument is that 
			//"connect Gandhi Setu with the major arterial and no where in between."
			Id<Link> linkId = Id.createLinkId("1572810000-1573110000-1573810000-15737-1572510000-1574810000-1574310000");
			Id<Link> reverseLinkId = Id.createLinkId("15743-15748-15725-1573710000-15738-15731-15728");
			network.removeLink(linkId);
			network.removeLink(reverseLinkId);
		}
	}
}
