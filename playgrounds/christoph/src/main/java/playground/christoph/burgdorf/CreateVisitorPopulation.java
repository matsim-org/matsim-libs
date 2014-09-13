/* *********************************************************************** *
 * project: org.matsim.*
 * CreateVisitorPopulation.java
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

public class CreateVisitorPopulation extends BurgdorfRoutes {
	
	private static final Logger log = Logger.getLogger(CreateVisitorPopulation.class);
	
//	private static String day = "freitag";
//	private static String day = "samstag";
	private static String day = "sonntag";
	
//	private static String direction = "to";
	private static String direction = "from";

//	private boolean viaKriegstetten = true;
//	private boolean viaSchoenbuehl = true;
	
	private boolean viaKriegstetten = false;
	private boolean viaSchoenbuehl = false;
	
	public static String networkFile = "../../matsim/mysimulations/burgdorf/input/network_burgdorf_cut.xml.gz";
	public static String populationFile = "../../matsim/mysimulations/burgdorf/input/plans_visitors_" + day + "_" + direction + "_burgdorf.xml.gz";
	
	/*
	 * The array below contain the expected arrival time but we have to set the departure times.
	 * Therefore let the agents depart earlier.
	 * ~ 15 minutes in the empty network
	 */
//	public static int timeShift = 1800;
	public static int timeShift = 0;
	
	// 48 entries - one for each 30 minutes
	public static int binSize = 1800;
		
//	public static int[] from1Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//	public static int[] from2Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//	public static int[] from3Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//	public static int[] from4Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//	public static int[] from5Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	// Samstag to Burgdorf
	public static int[] from1Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 63, 63, 188, 188, 250, 250, 119, 119, 32, 25, 19, 13, 13, 7, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from2Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 62, 62, 187, 187, 250, 250, 119, 119, 31, 25, 19, 12, 12, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from3Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 42, 42, 125, 125, 167, 167, 79, 79, 21, 17, 12, 8, 8, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from4Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 42, 42, 125, 125, 167, 167, 79, 79, 21, 17, 12, 8, 8, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from5Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 41, 41, 125, 125, 166, 166, 79, 79, 20, 16, 13, 9, 9, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	// Sonntag from Burgdorf
	public static int[] to1Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 125, 1250, 125, 125, 50, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] to2Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 125, 1250, 125, 125, 50, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] to3Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 83, 833, 83, 83, 33, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] to4Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 83, 833, 83, 83, 33, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] to5Departures = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 84, 834, 84, 84, 34, 0, 0, 0, 0, 0, 0, 0, 0};

	private List<Id<Link>> routeFrom1ToParkings;
	private List<Id<Link>> routeFrom2ToParkings;
	private List<Id<Link>> routeFrom3ToParkings;
	private List<Id<Link>> routeFrom4ToParkings;
	private List<Id<Link>> routeFrom5ToParkings;
	
	private List<Id<Link>> routeFromParkingsTo1;
	private List<Id<Link>> routeFromParkingsTo2;
	private List<Id<Link>> routeFromParkingsTo3;
	private List<Id<Link>> routeFromParkingsTo4;
	private List<Id<Link>> routeFromParkingsTo5;
	
	private int visitorCounter = 0;
	
	public static void main(String[] args) {
		new CreateVisitorPopulation();
	}
	
	public CreateVisitorPopulation() {
		
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
		 * to parking routes
		 */
		routeFrom1ToParkings = new ArrayList<Id<Link>>();
		if (!this.viaKriegstetten) {
			for (String id : from1) routeFrom1ToParkings.add(Id.create(id, Link.class));
			for (String id : highwayFromZurich) routeFrom1ToParkings.add(Id.create(id, Link.class));
		} else {
			for (String id : from1ToKriegstetten) routeFrom1ToParkings.add(Id.create(id, Link.class));
			for (String id : alternativeFromZurich) routeFrom1ToParkings.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom1ToParkings);
	
		routeFrom2ToParkings = new ArrayList<Id<Link>>();
		if (!this.viaKriegstetten) {
			for (String id : from2) routeFrom2ToParkings.add(Id.create(id, Link.class));
			for (String id : highwayFromZurich) routeFrom2ToParkings.add(Id.create(id, Link.class));			
		} else {
			for (String id : from1ToKriegstetten) routeFrom2ToParkings.add(Id.create(id, Link.class));
			for (String id : alternativeFromZurich) routeFrom2ToParkings.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom2ToParkings);
		
		routeFrom3ToParkings = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : from3) routeFrom3ToParkings.add(Id.create(id, Link.class));
			for (String id : highwayFromBern) routeFrom3ToParkings.add(Id.create(id, Link.class));			
		} else {
			for (String id : from3ToSchoenbuehl) routeFrom3ToParkings.add(Id.create(id, Link.class));
			for (String id : alternativeFromBern) routeFrom3ToParkings.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom3ToParkings);
		
		routeFrom4ToParkings = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : from4) routeFrom4ToParkings.add(Id.create(id, Link.class));
			for (String id : highwayFromBern) routeFrom4ToParkings.add(Id.create(id, Link.class));
		} else {
			for (String id : from4ToSchoenbuehl) routeFrom4ToParkings.add(Id.create(id, Link.class));
			for (String id : alternativeFromBern) routeFrom4ToParkings.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom4ToParkings);
		
		routeFrom5ToParkings = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : from5) routeFrom5ToParkings.add(Id.create(id, Link.class));
			for (String id : highwayFromBern) routeFrom5ToParkings.add(Id.create(id, Link.class));
		} else {
			for (String id : from5ToSchoenbuehl) routeFrom5ToParkings.add(Id.create(id, Link.class));
			for (String id : alternativeFromBern) routeFrom5ToParkings.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFrom5ToParkings);
		
		/*
		 * from parking routes
		 */
		routeFromParkingsTo1 = new ArrayList<Id<Link>>();
		if (!this.viaKriegstetten) {
			for (String id : parkingToHighwayToZurich) routeFromParkingsTo1.add(Id.create(id, Link.class));
			for (String id : to1) routeFromParkingsTo1.add(Id.create(id, Link.class));
		}
		else {
			for (String id : alternativeToZurich) routeFromParkingsTo1.add(Id.create(id, Link.class));
			for (String id : to1FromKriegstetten) routeFromParkingsTo1.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromParkingsTo1);

		routeFromParkingsTo2 = new ArrayList<Id<Link>>();
		if (!this.viaKriegstetten) {
			for (String id : parkingToHighwayToZurich) routeFromParkingsTo2.add(Id.create(id, Link.class));
			for (String id : to2) routeFromParkingsTo2.add(Id.create(id, Link.class));
		}
		else {
			for (String id : alternativeToZurich) routeFromParkingsTo2.add(Id.create(id, Link.class));
			for (String id : to2FromKriegstetten) routeFromParkingsTo2.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromParkingsTo2);

		routeFromParkingsTo3 = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : parkingToHighwayToBern) routeFromParkingsTo3.add(Id.create(id, Link.class));
			for (String id : to3) routeFromParkingsTo3.add(Id.create(id, Link.class));			
		} else {
			for (String id : alternativeToBern) routeFromParkingsTo3.add(Id.create(id, Link.class));
			for (String id : to3FromSchoenbuehl) routeFromParkingsTo3.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromParkingsTo3);
		
		routeFromParkingsTo4 = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : parkingToHighwayToBern) routeFromParkingsTo4.add(Id.create(id, Link.class));
			for (String id : to4) routeFromParkingsTo4.add(Id.create(id, Link.class));			
		} else {
			for (String id : alternativeToBern) routeFromParkingsTo4.add(Id.create(id, Link.class));
			for (String id : to4FromSchoenbuehl) routeFromParkingsTo4.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromParkingsTo4);
		
		routeFromParkingsTo5 = new ArrayList<Id<Link>>();
		if (!this.viaSchoenbuehl) {
			for (String id : parkingToHighwayToBern) routeFromParkingsTo5.add(Id.create(id, Link.class));
			for (String id : to5) routeFromParkingsTo5.add(Id.create(id, Link.class));			
		} else {
			for (String id : alternativeToBern) routeFromParkingsTo5.add(Id.create(id, Link.class));
			for (String id : to5FromSchoenbuehl) routeFromParkingsTo5.add(Id.create(id, Link.class));
		}
		checkRouteValidity(scenario, routeFromParkingsTo5);
	}
	
	// to Burgdorf
	private void createToPopulation(Scenario scenario) {
		createToRoutePopulation(scenario, 1, from1Departures, routeFrom1ToParkings);
		createToRoutePopulation(scenario, 2, from2Departures, routeFrom2ToParkings);
		createToRoutePopulation(scenario, 3, from3Departures, routeFrom3ToParkings);
		createToRoutePopulation(scenario, 4, from4Departures, routeFrom4ToParkings);
		createToRoutePopulation(scenario, 5, from5Departures, routeFrom5ToParkings);
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
				Person person = populationFactory.createPerson(Id.create("visitor_ " + visitorCounter + "_" + hourCounter + "_" + from + "_" + bin, Person.class));
				
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
			visitorCounter++;
		}
	}
	
	// from Burgdorf
	private void createFromPopulation(Scenario scenario) {
		List<Id<Link>> toZurichParkings = ParkingInfrastructure.availableParkings.get(Id.create("L30", Link.class));
		List<Id<Link>> toBernParkings = ParkingInfrastructure.availableParkings.get(Id.create("L01", Link.class));	
		
		createFromRoutePopulation(scenario, 1, to1Departures, routeFromParkingsTo1, toZurichParkings);
		createFromRoutePopulation(scenario, 2, to2Departures, routeFromParkingsTo2, toZurichParkings);
		createFromRoutePopulation(scenario, 3, to3Departures, routeFromParkingsTo3, toBernParkings);
		createFromRoutePopulation(scenario, 4, to4Departures, routeFromParkingsTo4, toBernParkings);
		createFromRoutePopulation(scenario, 5, to5Departures, routeFromParkingsTo5, toBernParkings);
	}
	
	private void createFromRoutePopulation(Scenario scenario, int to, int[] fromDepartures, List<Id<Link>> routeFromToParkings,
			List<Id<Link>> possibleParkings) {
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		
		int bin = 1;
		for (int departures : fromDepartures) {
			for (int hourCounter = 0; hourCounter < departures; hourCounter++) {
				Id<Link> parkingId = possibleParkings.get(MatsimRandom.getRandom().nextInt(possibleParkings.size()));
				List<Id<Link>> fromParkingSubRoute = ParkingInfrastructure.fromParkingSubRoutes.get(parkingId);
				
				List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
				routeLinkIds.addAll(fromParkingSubRoute);
				routeLinkIds.addAll(routeFromToParkings);
				
				Id<Link> fromLinkId = routeLinkIds.get(0);
				Id<Link> toLinkId = routeLinkIds.get(routeLinkIds.size() - 1);
				Route route = routeFactory.createRoute(TransportMode.car, fromLinkId, toLinkId);
				((NetworkRoute) route).setLinkIds(fromLinkId, routeLinkIds.subList(1, routeLinkIds.size() - 1), toLinkId);		

				Person person = populationFactory.createPerson(Id.create("visitor_ " + visitorCounter + "_" + hourCounter + "_" + to + "_" + bin, Person.class));
				
				Plan plan = populationFactory.createPlan();
				double departureTime = (bin - 1) * binSize + Math.round(MatsimRandom.getRandom().nextDouble() * binSize);
				
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
			visitorCounter++;
		}
	}
}
