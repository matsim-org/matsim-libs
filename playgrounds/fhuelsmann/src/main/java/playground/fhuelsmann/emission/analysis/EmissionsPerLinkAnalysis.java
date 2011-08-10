/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerLinkAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.fhuelsmann.emission.analysis;

import java.util.SortedSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;


/**
 * @author benjamin, friederike
 *
 */

public class EmissionsPerLinkAnalysis {
	private final static String runDirectory = "../../run980/";
	private final static String netFile = runDirectory + "980.output_network.xml.gz";
	
	private final static String plansFile = runDirectory + "980.output_plans.xml.gz";
	private final static String emissionFile = runDirectory + "emission.events.xml.gz";
	
	private static Scenario scenario;
	
	public static void main(String[] args) {
		loadScenario();
		
		Network network = scenario.getNetwork();
		EmissionsPerLinkAggregator epa = new EmissionsPerLinkAggregator(network,emissionFile);
		epa.run();
		
		SortedSet<String> listOfPollutants = epa.getListOfPollutants();
		EmissionWriter emissionWriter = new EmissionWriter(network);
		emissionWriter.writeLink2Emissions(
				network,
				listOfPollutants,
				epa.getWarmEmissions(),
				runDirectory +"output/emissionsWarmPerLink.txt");
		emissionWriter.writeLink2Emissions(
				network,
				listOfPollutants,
				epa.getColdEmissions(),
				runDirectory +"output/eemissionsColdPerLink.txt");
		emissionWriter.writeLink2Emissions(
				network,
				listOfPollutants,
				epa.getTotalEmissions(),
				runDirectory + "output/eemissionsTotalLink.txt");
	}
	
	private static void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
	}
}
