/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVisUtils
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
package playground.dgrether.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.vis.otfvis.data.OTFConnectionManager;


/**
 * @author dgrether
 *
 */
public class DgOTFVisUtils {
	
	private static final Logger log = Logger.getLogger(DgOTFVisUtils.class);
	
	public static void printConnectionManager(OTFConnectionManager c) {
		c.logEntries();
	}
	
	public static void locateAndRoutePopulation(MutableScenario scenario){
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new XY2Links(scenario));
		final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(
				new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
								scenario ).get(
						) ) );
		((PopulationImpl)scenario.getPopulation()).runAlgorithms();
	}
	
	public static void preparePopulation4Simulation(Scenario scenario) {
		final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		PlanAlgorithm router = 
				new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
								scenario ).get(
						) );
		PersonPrepareForSim pp4s = new PersonPrepareForSim(router, (MutableScenario) scenario);
		for (Person p : scenario.getPopulation().getPersons().values()){
			pp4s.run(p);
		}
	}

	

}
