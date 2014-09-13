/* *********************************************************************** *
 * project: org.matsim.*
 * CreateCampingPopulation.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateCampingPopulation extends BurgdorfRoutes {
	
	private static final Logger log = Logger.getLogger(CreateCampingPopulation.class);
	
//	private String day = "freitag";
	private String day = "samstag";
//	private String day = "sonntag";
	
//	private String direction = "to";
	private String direction = "from";
	
//	private boolean viaKriegstetten = true;	// this is already the default route!
	private boolean viaSchoenbuehl = true;
	
	private boolean viaKriegstetten = false;
//	private boolean viaSchoenbuehl = false;
	
	public String networkFile = "../../matsim/mysimulations/burgdorf/input/network_burgdorf_cut.xml.gz";
	public String populationFile = "../../matsim/mysimulations/burgdorf/input/plans_camping_" + day +  "_" + direction + "_burgdorf.xml.gz";

	/*
	 * The array below contain the expected arrival time but we have to set the departure times.
	 * Therefore let the agents depart earlier.
	 * ~ 15 minutes in the empty network
	 */
	public static int timeShift = 1800;
	
//	48 entries - one for each 30 minutes
	public static int binSize = 1800;
	
	// Samstag to Burgdorf
	public static int[] from1CampingDepartures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 19, 19, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from2CampingDepartures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 19, 19, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from3CampingDepartures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 12, 12, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from4CampingDepartures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 12, 12, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from5CampingDepartures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 13, 13, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	private List<Id<Link>> routeFrom1ToCamping;
	private List<Id<Link>> routeFrom2ToCamping;
	private List<Id<Link>> routeFrom3ToCamping;
	private List<Id<Link>> routeFrom4ToCamping;
	private List<Id<Link>> routeFrom5ToCamping;
	
	private List<Id<Link>> routeFromCampingTo1;
	private List<Id<Link>> routeFromCampingTo2;
	private List<Id<Link>> routeFromCampingTo3;
	private List<Id<Link>> routeFromCampingTo4;
	private List<Id<Link>> routeFromCampingTo5;
	
	private int campingCounter = 0;
	
	public static void main(String[] args) {
		new CreateCampingPopulation();
	}
	
	public CreateCampingPopulation() {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
//		config.counts().setCountsFileName(countsFile);
//		config.facilities().setInputFile(facilitiesFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		log.info("creating routes...");
		createRoutes(scenario);
		log.info("done.");
		
		log.info("creating population...");
		if (direction.equals("to")) createToPopulation(scenario);
		else createFromPopulation(scenario);
		log.info("done.");
		
		log.info("writing population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(populationFile);
		log.info("done.");
	}
	
	private void createRoutes(Scenario scenario) {

		/*
		 * to camping routes
		 */
		routeFrom1ToCamping = new ArrayList<Id<Link>>();
		if (!this.viaKriegstetten) {
			for (String id : from1) routeFrom1ToCamping.add(Id.create(id, Link.class));
			for (String id : alternativeFromZurichCamping) routeFrom1ToCamping.add(Id.create(id, Link.class));
		} else {
			for (String id : from1ToKriegstetten) routeFrom1ToCamping.add(Id.create(id, Link.class));
			for (String id : highwayFromZurichCamping) routeFrom1ToCamping.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom1ToCamping);
		
		routeFrom2ToCamping = new ArrayList<Id<Link>>();
		if (!this.viaKriegstetten) {
			for (String id : from2) routeFrom2ToCamping.add(Id.create(id, Link.class));
			for (String id : alternativeFromZurichCamping) routeFrom2ToCamping.add(Id.create(id, Link.class));
		} else {
			for (String id : from2ToKriegstetten) routeFrom2ToCamping.add(Id.create(id, Link.class));
			for (String id : highwayFromZurichCamping) routeFrom2ToCamping.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom2ToCamping);
		
		routeFrom3ToCamping = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : from3) routeFrom3ToCamping.add(Id.create(id, Link.class));
			for (String id : highwayFromBernCamping) routeFrom3ToCamping.add(Id.create(id, Link.class));
		} else {
			for (String id : from3ToSchoenbuehl) routeFrom3ToCamping.add(Id.create(id, Link.class));
			for (String id : alternativeFromBernCamping) routeFrom3ToCamping.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom3ToCamping);
		
		routeFrom4ToCamping = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : from4) routeFrom4ToCamping.add(Id.create(id, Link.class));
			for (String id : highwayFromBernCamping) routeFrom4ToCamping.add(Id.create(id, Link.class));
		} else {
			for (String id : from4ToSchoenbuehl) routeFrom4ToCamping.add(Id.create(id, Link.class));
			for (String id : alternativeFromBernCamping) routeFrom4ToCamping.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom4ToCamping);
		
		routeFrom5ToCamping = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : from5) routeFrom5ToCamping.add(Id.create(id, Link.class));
			for (String id : highwayFromBernCamping) routeFrom5ToCamping.add(Id.create(id, Link.class));
		} else {
			for (String id : from5ToSchoenbuehl) routeFrom5ToCamping.add(Id.create(id, Link.class));
			for (String id : alternativeFromBernCamping) routeFrom5ToCamping.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom5ToCamping);
		
		/*
		 * from camping routes
		 */
		routeFromCampingTo1 = new ArrayList<Id<Link>>();
		if (!this.viaKriegstetten) {
			for (String id : alternativeToZurichCamping) routeFromCampingTo1.add(Id.create(id, Link.class));
			for (String id : to1) routeFromCampingTo1.add(Id.create(id, Link.class));
		} else {
			for (String id : highwayToZurichCamping) routeFromCampingTo1.add(Id.create(id, Link.class));
			for (String id : to1FromKriegstetten) routeFromCampingTo1.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromCampingTo1);
		
		routeFromCampingTo2 = new ArrayList<Id<Link>>();
		if (!this.viaKriegstetten) {
			for (String id : alternativeToZurichCamping) routeFromCampingTo2.add(Id.create(id, Link.class));
			for (String id : to2) routeFromCampingTo2.add(Id.create(id, Link.class));
		} else {
			for (String id : highwayToZurichCamping) routeFromCampingTo2.add(Id.create(id, Link.class));
			for (String id : to2FromKriegstetten) routeFromCampingTo2.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromCampingTo2);
		
		routeFromCampingTo3 = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : highwayToBernCamping) routeFromCampingTo3.add(Id.create(id, Link.class));
			for (String id : to3) routeFromCampingTo3.add(Id.create(id, Link.class));
		} else {
			for (String id : alternativeToBernCamping) routeFromCampingTo3.add(Id.create(id, Link.class));
			for (String id : to3FromSchoenbuehl) routeFromCampingTo3.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromCampingTo3);
		
		routeFromCampingTo4 = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : highwayToBernCamping) routeFromCampingTo4.add(Id.create(id, Link.class));
			for (String id : to4) routeFromCampingTo4.add(Id.create(id, Link.class));
		} else {
			for (String id : alternativeToBernCamping) routeFromCampingTo4.add(Id.create(id, Link.class));
			for (String id : to4FromSchoenbuehl) routeFromCampingTo4.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromCampingTo4);
		
		routeFromCampingTo5 = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : highwayToBernCamping) routeFromCampingTo5.add(Id.create(id, Link.class));
			for (String id : to5) routeFromCampingTo5.add(Id.create(id, Link.class));
		} else {
			for (String id : alternativeToBernCamping) routeFromCampingTo5.add(Id.create(id, Link.class));
			for (String id : to5FromSchoenbuehl) routeFromCampingTo5.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromCampingTo5);
	}
	
	private void createToPopulation(Scenario scenario) {
		
		createToRoutePopulation(scenario, 1, from1CampingDepartures, routeFrom1ToCamping);
		createToRoutePopulation(scenario, 2, from2CampingDepartures, routeFrom2ToCamping);
		createToRoutePopulation(scenario, 3, from3CampingDepartures, routeFrom3ToCamping);
		createToRoutePopulation(scenario, 4, from4CampingDepartures, routeFrom4ToCamping);
		createToRoutePopulation(scenario, 5, from5CampingDepartures, routeFrom5ToCamping);
	}
	
	private void createToRoutePopulation(Scenario scenario, int from, int[] fromDepartures, List<Id<Link>> routeFromToParkings) {
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		
		int bin = 1;
		Id<Link> fromLinkId = routeFromToParkings.get(0);
		Id<Link> toLinkId = routeFromToParkings.get(routeFromToParkings.size() - 1);
		Route route = routeFactory.createRoute(TransportMode.car, fromLinkId, toLinkId);
		((NetworkRoute) route).setLinkIds(fromLinkId, routeFromToParkings.subList(1, routeFromToParkings.size() - 1), toLinkId);		
		for (int departures : fromDepartures) {
			for (int hourCounter = 0; hourCounter < departures; hourCounter++) {
				Person person = populationFactory.createPerson(Id.create("camping_" + campingCounter + "_" + hourCounter + "_" + from + "_" + bin, Person.class));
				
				Plan plan = populationFactory.createPlan();
				double departureTime = (bin - 1) * binSize + Math.round(MatsimRandom.getRandom().nextDouble() * binSize);
				departureTime -= timeShift;
				
				Activity fromActivity = populationFactory.createActivityFromLinkId("home", fromLinkId);
				fromActivity.setEndTime(departureTime);
				
				Leg leg = populationFactory.createLeg(TransportMode.car);
				leg.setDepartureTime(departureTime);
				leg.setRoute(route);
				
				Activity toActivity = populationFactory.createActivityFromLinkId("leisure", toLinkId);
				
				plan.addActivity(fromActivity);
				plan.addLeg(leg);
				plan.addActivity(toActivity);
				
				person.addPlan(plan);
				
				scenario.getPopulation().addPerson(person);				
			}
				
			bin++;
			campingCounter++;
		}
	}
	
	private void createFromPopulation(Scenario scenario) {
		
		createFromRoutePopulation(scenario, 1, from1CampingDepartures, routeFrom1ToCamping);
		createFromRoutePopulation(scenario, 2, from2CampingDepartures, routeFrom2ToCamping);
		createFromRoutePopulation(scenario, 3, from3CampingDepartures, routeFrom3ToCamping);
		createFromRoutePopulation(scenario, 4, from4CampingDepartures, routeFrom4ToCamping);
		createFromRoutePopulation(scenario, 5, from5CampingDepartures, routeFrom5ToCamping);
	}
	
	private void createFromRoutePopulation(Scenario scenario, int from, int[] fromDepartures, List<Id<Link>> routeFromToParkings) {
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		
		int bin = 1;
		Id<Link> fromLinkId = routeFromToParkings.get(0);
		Id<Link> toLinkId = routeFromToParkings.get(routeFromToParkings.size() - 1);
		Route route = routeFactory.createRoute(TransportMode.car, fromLinkId, toLinkId);
		((NetworkRoute) route).setLinkIds(fromLinkId, routeFromToParkings.subList(1, routeFromToParkings.size() - 1), toLinkId);		
		for (int departures : fromDepartures) {
			for (int hourCounter = 0; hourCounter < departures; hourCounter++) {
				Person person = populationFactory.createPerson(Id.create("camping_" + campingCounter + "_" + hourCounter + "_" + from + "_" + bin, Person.class));
				
				Plan plan = populationFactory.createPlan();
				double departureTime = (bin - 1) * binSize + Math.round(MatsimRandom.getRandom().nextDouble() * binSize);
				departureTime -= timeShift;
				
				Activity fromActivity = populationFactory.createActivityFromLinkId("home", fromLinkId);
				fromActivity.setEndTime(departureTime);
				
				Leg leg = populationFactory.createLeg(TransportMode.car);
				leg.setDepartureTime(departureTime);
				leg.setRoute(route);
				
				Activity toActivity = populationFactory.createActivityFromLinkId("leisure", toLinkId);
				
				plan.addActivity(fromActivity);
				plan.addLeg(leg);
				plan.addActivity(toActivity);
				
				person.addPlan(plan);
				
				scenario.getPopulation().addPerson(person);				
			}
				
			bin++;
			campingCounter++;
		}
	}
}
