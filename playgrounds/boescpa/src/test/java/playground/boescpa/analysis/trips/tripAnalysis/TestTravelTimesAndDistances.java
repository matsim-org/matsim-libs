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

package playground.boescpa.analysis.trips.tripAnalysis;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.boescpa.analysis.trips.EventsToTrips;
import playground.boescpa.analysis.trips.Trip;
import playground.boescpa.analysis.trips.TripWriter;

import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author pboesch
 *
 */
public class TestTravelTimesAndDistances {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testTripProcessing() {
		// Run Scenario "equil"
		final Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.setParam("controler", "outputDirectory", utils.getOutputDirectory());

		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		// (otherwise the numbers change; the simulation may still be correct. kai, feb'16)  
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		controler.run();

        // Load events file
        String eventsFile = this.utils.getOutputDirectory() + "ITERS/it.0/0.events.xml.gz";
        List<Trip> trips = EventsToTrips.createTripsFromEvents(eventsFile, scenario.getNetwork());
		
		// run postprocessing
        new TripWriter().writeTrips(trips, this.utils.getOutputDirectory() + "tripResults.txt");
        HashMap<String, Double[]> results = TravelTimesAndDistances.calcTravelTimeAndDistance(trips, this.utils.getOutputDirectory() + "analResults.txt");
        Double[] car = results.get("car");
        Double[] pt = results.get("pt");
        Double[] transit_walk = results.get("transit_walk");

		// TripProcessing.analyzeTrips - Time tests
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - car mode time sum not as expected.", 442532, car[0], 0);
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - pt mode time sum not as expected.", 1540528, pt[0], 0);
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - transit_walk mode time sum not as expected.", 1236305, transit_walk[0], 0);
		// TripProcessing.analyzeTrips - Distance tests
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - car mode distance sum not as expected.", 5256000.00, car[1], 0);
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - pt mode distance sum not as expected.", 2334138.27, pt[1], 0.01);
			// Test above expects calculation of pt distances by euclidian distance. Should this change, this value needs to be readjusted...
		Assert.assertEquals("Test: TripProcessing.analyzeTrips - transit_walk mode distance sum not as expected.", 953810.68, transit_walk[1], 0.01);
			// Test above expects calculation of transit_walk distances by euclidian distance. Should this change, this value needs to be readjusted...
	}
}
