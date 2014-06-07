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

package playground.boescpa.topdad;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.boescpa.topdad.postprocessing.TripHandler;
import playground.boescpa.topdad.postprocessing.TripProcessing;

/**
 * 
 * @author pboesch
 *
 */
public class TestMainTripCreator {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testTripProcessing() {
		// Run Scenario "equil"
		final Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.setParam("controler", "outputDirectory", utils.getOutputDirectory());
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		controler.run();
		
		// Get network
		Network network = scenario.getNetwork();
		
		// Register events handler
		EventsManager events = EventsUtils.createEventsManager();
		TripHandler tripHandler = new TripHandler();
		events.addHandler(tripHandler);
		tripHandler.reset(0);
		
		// Load events file
		String eventsFile = this.utils.getOutputDirectory() + "ITERS/it.10/10.events.xml.gz";
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		
		// run postprocessing
		TripProcessing.printTrips(tripHandler, network, this.utils.getOutputDirectory() + "tripResults.txt");
		HashMap<String, Double[]> results =
				TripProcessing.analyzeTripsTopdad(tripHandler, network, this.utils.getOutputDirectory() + "analResults.txt");
		
		// TripProcessing.analyzeTrips - Time tests
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - car mode time sum not as expected.", 10147.12, results.get("car")[0], 0.01);
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - pt mode time sum not as expected.", 15227.68, results.get("pt")[0], 0.01);
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - transit_walk mode time sum not as expected.", 13114.93, results.get("transit_walk")[0], 0.01);
		// TripProcessing.analyzeTrips - Distance tests
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - car mode distance sum not as expected.", 7231200.0, results.get("car")[1], 0);
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - pt mode distance sum not as expected.", 963017.0, results.get("pt")[1], 0);
			// Test above expects calculation of pt distances by euclidian distance. Should this change, this value needs to be readjusted...
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - transit_walk mode distance sum not as expected.", 430244.0, results.get("transit_walk")[1], 0);
			// Test above expects calculation of transit_walk distances by euclidian distance. Should this change, this value needs to be readjusted...
	}
}
