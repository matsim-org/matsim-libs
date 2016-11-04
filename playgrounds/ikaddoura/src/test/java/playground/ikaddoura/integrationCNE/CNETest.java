/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.integrationCNE;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.integrationCNE.CNEControler.CaseStudy;

/**
 * @author ikaddoura
 *
 */

public class CNETest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Ignore
	@Test
	public final void test1(){

		String configFile = testUtils.getPackageInputDirectory() + "CNETest/config.xml";

		// baseCase
		CNEControler cnControler1 = new CNEControler();
		String outputDirectory1 = testUtils.getOutputDirectory() + "bc";
		cnControler1.run(configFile, outputDirectory1, false, false, false, CaseStudy.Test);
		
		// only air pollution pricing
		CNEControler cnControler2 = new CNEControler();
		String outputDirectory2 = testUtils.getOutputDirectory() + "e";
		cnControler2.run(configFile, outputDirectory2, false, false, true, CaseStudy.Test);
		
		// only noise pricing
		CNEControler cnControler3 = new CNEControler();
		String outputDirectory3 = testUtils.getOutputDirectory() + "cne";
		cnControler3.run(configFile, outputDirectory3, true, true, true, CaseStudy.Test);
	
		// analyze output events file

		LinkDemandEventHandler handler1 = analyzeEvents(outputDirectory1, configFile); // base case
		LinkDemandEventHandler handler2 = analyzeEvents(outputDirectory2, configFile); // air pollution pricing
		LinkDemandEventHandler handler3 = analyzeEvents(outputDirectory3, configFile); // noise + air pollution pricing
		
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults(handler1);
		System.out.println("----------------------------------");
		System.out.println("Air pollution pricing:");
		printResults(handler2);
		System.out.println("----------------------------------");
		System.out.println("Noise and Air pollution pricing:");
		printResults(handler3);
	}

	private void printResults(LinkDemandEventHandler handler) {
		System.out.println("high speed + low N costs + high E costs: " + demand_highSpeed_lowN_highE(handler));
		System.out.println("low speed + low N costs + low E costs: " + demand_lowSpeed_lowN_lowE(handler));
		System.out.println("medium speed + high N costs + low E costs: " + demand_mediumSpeed_highN_lowE(handler));
	}

	private int demand_mediumSpeed_highN_lowE(LinkDemandEventHandler handler) {
		int demand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_7_8"))) {
			demand = handler.getLinkId2demand().get(Id.createLinkId("link_7_8"));
		}
		return demand;
	}

	private int demand_lowSpeed_lowN_lowE(LinkDemandEventHandler handler) {
		int demand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_3_6"))) {
			demand = handler.getLinkId2demand().get(Id.createLinkId("link_3_6"));
		}
		return demand;
	}

	private int demand_highSpeed_lowN_highE(LinkDemandEventHandler handler) {
		int demand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_1_2"))) {
			demand = handler.getLinkId2demand().get(Id.createLinkId("link_1_2"));
		}
		return demand;
	}

	private LinkDemandEventHandler analyzeEvents(String outputDirectory, String configFile) {

		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
				
		LinkDemandEventHandler handler = new LinkDemandEventHandler(scenario.getNetwork());
		eventsManager.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(outputDirectory + "/ITERS/it.10/10.events.xml.gz");
		
		return handler;
	}
		
}
