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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PersonPrepareForSim;
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
	
	public static void locateAndRoutePopulation(Scenario scenario){
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new XY2Links((NetworkImpl) scenario.getNetwork()));
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(scenario.getConfig().planCalcScore());
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalc, timeCostCalc, new DijkstraFactory(), ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory()));
		((PopulationImpl)scenario.getPopulation()).runAlgorithms();
	}
	
	public static void preparePopulation4Simulation(Scenario scenario) {
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(scenario.getConfig().planCalcScore());
		PlansCalcRoute router = new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalc, timeCostCalc, new DijkstraFactory(), ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory());
		PersonPrepareForSim pp4s = new PersonPrepareForSim(router, (NetworkImpl) scenario.getNetwork());
		for (Person p : scenario.getPopulation().getPersons().values()){
			pp4s.run(p);
		}
	}

	

}
