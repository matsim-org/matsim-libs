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

package playground.ikaddoura.analysis.shapes;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.congestion.analysis.CongestionAnalysisEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;

/**
 * 
 * @author ikaddoura
 *
 */
public class IKGISAnalyzerMain {
	
	private final String runDirectory1 = "../../runs-svn/berlin_internalizationCar2/output/baseCase_2/";
	private final String shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_1500/berlin_grid_1500.shp";
		
	private final String homeActivity = "home";
	private final String workActivity = "work";

	// the number of persons a single agent represents
	final int scalingFactor = 10;
	
	private static final Logger log = Logger.getLogger(IKGISAnalyzerMain.class);
	
	public static void main(String[] args) throws IOException {
		IKGISAnalyzerMain main = new IKGISAnalyzerMain();
		main.run();
	}
		
	public void run() throws IOException {
		
		log.info("Loading scenario...");
		Scenario scenario1 = loadScenario();
		PopulationReader mpr = new PopulationReader(scenario1);
		mpr.readFile(runDirectory1 + "output_plans.xml.gz");
		log.info("Loading scenario... Done.");
		
		EventsManager events = EventsUtils.createEventsManager();

		// Compute marginal congestion events based on normal events file.
		CongestionHandlerImplV3 congestionHandler = new CongestionHandlerImplV3(events, (MutableScenario) scenario1);
		events.addHandler(congestionHandler);
		
		// Analyze external cost per person based on marginal congestion events.
		CongestionAnalysisEventHandler extCostHandler = new CongestionAnalysisEventHandler(scenario1, false);
		events.addHandler(extCostHandler);
		
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		String eventsFile1 = runDirectory1 + "/ITERS/it." + scenario1.getConfig().controler().getLastIteration() + "/" + scenario1.getConfig().controler().getLastIteration() + ".events.xml.gz";
		reader.readFile(eventsFile1);
		log.info("Reading events file... Done.");
		
		// Spatial analysis
		Map<Id<Person>, Double> causingAgentId2amountSum = extCostHandler.getCausingAgentId2amountSumAllAgents();
		Map<Id<Person>, Double> affectedAgentId2amountSum = extCostHandler.getAffectedAgentId2amountSumAllAgents();

		log.info("Analyzing zones...");
		IKGISAnalyzer gisAnalysis = new IKGISAnalyzer(shapeFileZones, scalingFactor, homeActivity, workActivity);
		gisAnalysis.analyzeZones_congestionCost(scenario1, runDirectory1, causingAgentId2amountSum, affectedAgentId2amountSum);
		log.info("Analyzing zones... Done.");
	}
	
	private Scenario loadScenario() {
		Config config = ConfigUtils.loadConfig(runDirectory1 + "output_config.xml.gz");
		config.network().setInputFile(runDirectory1 + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory1 + "output_plans.xml.gz");
//		config.plans().setInputFile(null);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
		
}
