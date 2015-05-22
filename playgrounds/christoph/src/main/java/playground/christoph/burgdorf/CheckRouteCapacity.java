/* *********************************************************************** *
 * project: org.matsim.*
 * CheckRouteCapacity.java
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

package playground.christoph.burgdorf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Creates a population that travel from/to Burgdorf to check the capacity
 * of the alternative routes from/to the highways.
 * 
 * @author cdobler
 */
public class CheckRouteCapacity {

	private String configFile = "../../matsim/mysimulations/burgdorf/config_burgdorf.xml";
	private String outputPath = "../../matsim/mysimulations/burgdorf/output";
	
	// agents per hour and direction
	private int agentsPerHour = 2500;
	private int hours = 4;
	
	private boolean toBurgdorf = false;
	
	public static void main(String[] args) {
		new CheckRouteCapacity();
	}
	
	public CheckRouteCapacity() {
		
		if (toBurgdorf) outputPath += "_toBurgdorf";
		else outputPath += "_fromBurgdorf";
		outputPath += "_Volume_";
		outputPath += agentsPerHour;
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outputPath);
		config.network().setTimeVariantNetwork(false);
		config.plans().setInputFile(null);
		
		if (toBurgdorf) config.controler().setRunId("to_" + agentsPerHour);
		else config.controler().setRunId("from_" + agentsPerHour);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		createPopulation(scenario);
		
		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}
	
	private void createPopulation(Scenario scenario) {
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		Random random = MatsimRandom.getLocalInstance();
		
		List<Id<Link>> routeIdsKriegstetten = new ArrayList<Id<Link>>();
		List<Id<Link>> routeIdsSchoenbuehl = new ArrayList<Id<Link>>();
		
		if (toBurgdorf) {
			for (String id : BurgdorfRoutes.alternativeFromZurich) routeIdsKriegstetten.add(Id.create(id, Link.class));
			for (String id : BurgdorfRoutes.alternativeFromBern) routeIdsSchoenbuehl.add(Id.create(id, Link.class));
		} else {
			for (String id : BurgdorfRoutes.alternativeToZurich) routeIdsKriegstetten.add(Id.create(id, Link.class));
			for (String id : BurgdorfRoutes.alternativeToBern) routeIdsSchoenbuehl.add(Id.create(id, Link.class));
		}

		NetworkRoute routeKriegstetten;
		NetworkRoute routeSchoenbuehl;
		Id<Link> startLinkId;
		Id<Link> endLinkId;
		
		startLinkId = routeIdsKriegstetten.get(0);
		endLinkId = routeIdsKriegstetten.get(routeIdsKriegstetten.size() - 1);
		routeKriegstetten = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(startLinkId, endLinkId);
		routeKriegstetten.setLinkIds(startLinkId, routeIdsKriegstetten.subList(1, routeIdsKriegstetten.size() - 1), endLinkId);
		
		startLinkId = routeIdsSchoenbuehl.get(0);
		endLinkId = routeIdsSchoenbuehl.get(routeIdsSchoenbuehl.size() - 1);
		routeSchoenbuehl = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(startLinkId, endLinkId);
		routeSchoenbuehl.setLinkIds(startLinkId, routeIdsSchoenbuehl.subList(1, routeIdsSchoenbuehl.size() - 1), endLinkId);
		
		for (int hour = 0; hour < hours; hour++) {
			for (int agent = 0; agent < agentsPerHour; agent++) {

				double departureTime = hour * 3600 + Math.round(random.nextDouble() * 3600.0);
				
				Person person;
				Plan plan;
				Activity fromActivity;
				Leg leg;
				Activity toActivity;
				Id fromLinkId;
				Id toLinkId;
				
				// via Kriegstetten
				person = populationFactory.createPerson(Id.create("Hour_" + hour + 
						"_Agent_" + agent + "_Direction_Kriegstetten", Person.class));
				
				plan = populationFactory.createPlan();
				fromLinkId = routeKriegstetten.getStartLinkId();
				toLinkId = routeKriegstetten.getEndLinkId();
				
				fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
				fromActivity.setEndTime(departureTime);
				
				leg = populationFactory.createLeg(TransportMode.car);
				leg.setDepartureTime(departureTime);
				leg.setRoute(routeKriegstetten);
				
				toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
				
				plan.addActivity(fromActivity);
				plan.addLeg(leg);
				plan.addActivity(toActivity);
				
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
				
				// via Schoenbuehl
				person = populationFactory.createPerson(Id.create("Hour_" + hour + 
						"_Agent_" + agent + "_Direction_Schoenbuehl", Person.class));
				
				plan = populationFactory.createPlan();
				fromLinkId = routeSchoenbuehl.getStartLinkId();
				toLinkId = routeSchoenbuehl.getEndLinkId();
				
				fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
				fromActivity.setEndTime(departureTime);
				
				leg = populationFactory.createLeg(TransportMode.car);
				leg.setDepartureTime(departureTime);
				leg.setRoute(routeSchoenbuehl);
				
				toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
				
				plan.addActivity(fromActivity);
				plan.addLeg(leg);
				plan.addActivity(toActivity);
				
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
			}
		}
	}
}
