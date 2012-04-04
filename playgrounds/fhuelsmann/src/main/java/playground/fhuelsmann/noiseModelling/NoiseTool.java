/* *********************************************************************** *
 * project: org.matsim.*
 * NoiseTool.java
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

package playground.fhuelsmann.noiseModelling;

import java.io.IOException;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;


public class NoiseTool {
	private static String runDirectory = "../../run981/";
	private static String eventsFile = runDirectory + "ITERS/it.1500/981.1500.events.xml.gz";
	private static String netfile = runDirectory + "981.output_network.xml.gz";
	private final Scenario scenario;
	
	String outputfile = runDirectory + "noiseEvents.xml";

	
	public NoiseTool() {
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		NoiseTool noise = new NoiseTool();
		noise.run(args);
	}

	
	private void run(String[] args) {

		loadScenario();
		Network network = scenario.getNetwork();
	
		/* start events processing*/
		EventsManager eventsManager = EventsUtils.createEventsManager();
		NoiseHandler handler = new NoiseHandler(network);
		eventsManager.addHandler(handler);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		/* get information from class NoiseHandler  and save it in the following maps: linkId, time, traffic info*/
		Map<Id, Map<String, double[]>> linkInfos = handler.getlinkId2timePeriod2TrafficInfo();
		Map <Id,double [][]> linkInfosProStunde = handler.getlinkId2hour2vehicles();
	
		/* new instance of the class Calculation*/
		Calculation calculation = new Calculation();
		
		/* get information from class Calculation using the respective maps (linkInfos and res1) as input*/
		Map <Id,Map<String,Double>> linkId2timePeriod2lme = calculation.calculate_lme(linkInfos);
		Map <Id,Double> linkId2Lden = calculation.cal_lden(linkId2timePeriod2lme);
		
		/*new instance of the class NoiseWriter */
		NoiseWriter writer = new NoiseWriter (linkId2timePeriod2lme, linkId2Lden);
		
				
		writer.writeEvents();
		
		try { // write the TrafficInfos per hour for every link 
			writer.writeInfosProStunde(linkInfosProStunde);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			

	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netfile);
		// config.plans().setInputFile(plansfile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();

	}

}