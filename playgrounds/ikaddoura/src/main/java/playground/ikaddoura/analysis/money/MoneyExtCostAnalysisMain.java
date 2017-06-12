/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.money;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.noise.personLinkMoneyEvents.CombinedPersonLinkMoneyEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author ikaddoura
 *
 */
public class MoneyExtCostAnalysisMain {

//	static String outputDirectory = "/Users/ihab/Desktop/ils4a/kaddoura/cne/munich/output/output_run4_muc_cne_DecongestionPID/";
//	static String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output_final/m_r_output_run4_bln_cne_DecongestionPID/";
	static String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_scenario_2016/be_127/";

	public static void main(String[] args) {
		MoneyExtCostAnalysisMain anaMain = new MoneyExtCostAnalysisMain();
		anaMain.run();
		
	}

	private void run() {
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
	
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile(outputDirectory + "output_network.xml.gz");
		config.network().setInputFile(outputDirectory + "be_127.output_network.xml.gz");
		Network network = ScenarioUtils.loadScenario(config).getNetwork();
		
		MoneyExtCostHandler handler = new MoneyExtCostHandler(network);
		eventsManager.addHandler(handler);
		
		CombinedPersonLinkMoneyEventsReader reader = new CombinedPersonLinkMoneyEventsReader(eventsManager);
//		reader.readFile(outputDirectory + "output_events.xml.gz");
		reader.readFile(outputDirectory + "be_127.output_events.xml.gz");		
//		reader.readFile(outputDirectory + "ITERS/it.1500/1500.events.xml.gz");
		
		handler.writeInfo(outputDirectory);
	} 
}
		

