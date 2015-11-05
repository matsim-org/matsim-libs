/* *********************************************************************** *
 * project: org.matsim.*
 * PreparePopulation.java
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

package playground.christoph.strc2014;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.parking.core.utils.LegModeChecker;

import javax.inject.Provider;

/**
 * Prepares the population for the parking simulation. It ensures that:
 * - Vehicles are available where agents start their car trips, i.e. they are not teleported anymore.
 * - It is ensured, that the "activity -> link" and "facility -> link" mapping is valid. 
 * 
 * @author cdobler
 */
public class PreparePopulation {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks);
		config.network().setInputFile("../../matsim/mysimulations/parking/zurich/input/network_ivtch.xml.gz");
		config.setParam(WorldConnectLocations.CONFIG_F2L, WorldConnectLocations.CONFIG_F2L_INPUTF2LFile, "../../matsim/mysimulations/parking/zurich/input/f2l_ivtch.txt");
		config.facilities().setInputFile("../../matsim/mysimulations/parking/zurich/input/facilities_with_parking_ivtch.xml.gz");
		config.plans().setInputFile("../../matsim/mysimulations/parking/zurich/input/plans_census2000v2_dilZh30km_10pct_ivtch.xml.gz");
//		config.plans().setInputFile("../../matsim/mysimulations/parking/zurich/input/plans_debug.xml");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// connect facilities to links
		new WorldConnectLocations(config).connectFacilitiesWithLinks(scenario.getActivityFacilities(), (NetworkImpl) scenario.getNetwork());
		
		fixActivityFacilityMapping(scenario);
		
		fixModeChains(scenario);
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("../../matsim/mysimulations/parking/zurich/input/plans_census2000v2_dilZh30km_10pct_ivtch_prepared.xml.gz");
//		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("../../matsim/mysimulations/parking/zurich/input/plans_debug_prepared.xml");
	}
	
	/*
	 * Fix a problem occurring when agents' activites are attached to links that do not match
	 * the links where the facilities where the activities are performed are attached to.
	 * Moreover, remove all routes.
	 */
	private static void fixActivityFacilityMapping(Scenario scenario) {
		Counter counter = new Counter("# fixed persons: ");
		PlanAlgorithm planAlgorithm = getPlanRouter(scenario);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					ActivityImpl activity = (ActivityImpl) planElement;
					// use the facilities linkId
					activity.setLinkId(scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()).getLinkId());
				} else if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					leg.setRoute(null);
				}
			}
			planAlgorithm.run(person.getSelectedPlan());
			counter.incCounter();
		}
		counter.printCounter();
	}
	
	/*
	 * Fix a problem occurring when agents have invalid mode chains where the park a car at link A
	 * but want to use it later at link B. 
	 */
	private static void fixModeChains(Scenario scenario) {
		PlanAlgorithm planAlgorithm = getPlanRouter(scenario);
		LegModeChecker legModeChecker = new LegModeChecker(scenario, planAlgorithm);
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(scenario.getPopulation());
	}
	
	private static PlanAlgorithm getPlanRouter(Scenario scenario) {
//		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = 
//		new TripRouterFactoryBuilderWithDefaults().createDefaultLeastCostPathCalculatorFactory(scenario);

		Provider<TripRouter> tripRouterFactory;
		Provider<TripRouter> defaultTripRouterFactory = new TripRouterFactoryBuilderWithDefaults().build(scenario);
//		TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(scenario, multiModalTravelTimes, 
//		controler.getTravelDisutilityFactory(), defaultTripRouterFactory, leastCostPathCalculatorFactory);
		tripRouterFactory = defaultTripRouterFactory;
		
 		// ensure that all agents' plans have valid mode chains
		TravelTime travelTime = new FreeSpeedTravelTime();
		
		TravelDisutilityFactory travelDisutilityFactory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime, scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);
		TripRouter tripRouter = tripRouterFactory.get();
		
		return new PlanRouter(tripRouter);
	}
}
