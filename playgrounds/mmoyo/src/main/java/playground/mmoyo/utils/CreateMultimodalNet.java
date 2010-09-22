/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.utils;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;

import playground.mrieser.pt.utils.MergeNetworks;

/**Adds the ptlinks from the pseudonetwork/transitschedule to the network*/
public class CreateMultimodalNet {

	public void run(ScenarioImpl scenario, final String outputFile) {
		NetworkImpl ptNetwork = NetworkImpl.createNetwork();
		new CreatePseudoNetwork(scenario.getTransitSchedule(), ptNetwork, "tr_").createNetwork();
		MergeNetworks.merge(scenario.getNetwork(), "", ptNetwork, "", scenario.getNetwork());
		new NetworkWriter(scenario.getNetwork()).write(outputFile);
		System.out.println("done writting:" + outputFile);
		// NetVis.displayNetwork(outputFile);
	}

	public static void main(String[] args) {
		String configFile;

		if (args.length==1){
			configFile=args[0];
		}else{
			configFile = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/transfer/config_withoutPT.xml";
		}
		ScenarioImpl scenario = new TransScenarioLoader().loadScenarioWithTrSchedule(configFile);
		new CreateMultimodalNet().run(scenario, scenario.getConfig().controler().getOutputDirectory() + "/multimodalNetwork.xml");
	}

}