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

public class CreateVisitorPopulation {
	
	private static final Logger log = Logger.getLogger(CreateVisitorPopulation.class);
	
	public static String networkFile = "../../matsim/mysimulations/burgdorf/input/network_burgdorf.xml";
	public static String populationFile = "../../matsim/mysimulations/burgdorf/input/plans_visitors.xml.gz";

//	#1
	public static String[] from1 = new String[]{"17560003127350FT", "17560001067493TF", "17560000194597FT", "17560001985153FT",
				"17560001346037FT", "17560000194892FT", "17560001856301FT", "17560001357928FT", "17560001357914FT", "17560001999546FT",
				"17560000269138FT", "17560001885754FT", "17560001381024FT", "17560001985481FT", "17560000269878FT", "17560000269897FT",
				"17560000150173FT", "17560000150179FT", "17560002228032FT"};

//	#2
	public static String[] from2 = new String[]{"17560002104492FT", "17560001811758FT", "17560002104481FT", "17560001243331FT",
				"17560001811760FT", "17560001811776FT", "17560001811806FT", "17560001885754FT", "17560001381024FT", "17560001985481FT",
				"17560000269878FT", "17560000269897FT", "17560000150173FT", "17560000150179FT", "17560002228032FT"};
	
//	#3
	public static String[] from3 = new String[]{"17560000116600TF", "17560001985055TF", "17560000118607FT", "17560001985071FT", 
				"17560000118970FT", "17560002102697FT", "17560002102698FT", "17560001985497TF", "17560000149269FT", "17560001840179FT",
				"17560002102704FT", "17560002014557FT", "17560001813096TF", "17560001529979FT", "17560000142885FT", "17560000142879FT_A",
				"17560000142879FT_B", "17560000142879FT_D", "17560001849337FT_A", "17560001849337FT_A"};

//	#4
	public static String[] from4 = new String[]{"17560001814298FT", "17560001368904FT", "17560001813063FT", "17560001813064FT",
				"17560001368906FT", "17560001368907FT", "17560000126294FT", "17560001812171FT", "17560001834647FT", "17560002102722FT",
				"17560001813075FT", "17560001812069TF", "17560001853018TF", "17560000131237TF", "17560000131238TF", "17560000149624FT", 
				"17560001813087FT", "17560001813096TF", "17560001529979FT", "17560000142885FT", "17560000142879FT_A", "17560000142879FT_B",
				"17560000142879FT_D", "17560001849337FT_A", "17560001849337FT_A"};
	
//	#5
	public static String[] from5 = new String[]{"17560000130806FT", "17560001813912FT", "17560001813913FT", "17560002100968FT", 
				"17560000122288FT", "17560000122328FT", "17560001368986TF", "17560000122408TF", "17560000122648TF", "17560002077641TF",
				"17560001812069TF", "17560001853018TF", "17560000131237TF", "17560000131238TF", "17560000149624FT", "17560001813087FT", 
				"17560001813096TF", "17560001529979FT", "17560000142885FT", "17560000142879FT_A", "17560000142879FT_B", "17560000142879FT_D",
				"17560001849337FT_A", "17560001849337FT_A"};
	
	public static String[] highwayFromZurich = new String[] {"17560002078855TF", "17560001368695TF", "17560002070162TF",
				"17560002072150TF", "17560002104075TF", "17560002104075TF-1", "17560001368827FT", "17560001368863FT", "17560001368865FT",
				"17560001368861FT", "17560001368862FT", "17560001368862FT-8", "17560000143007FT", "L29", "L30"};
	
	public static String[] highwayFromBern = new String[] {"17560001849338FT", "17560001368756FT", "17560002104075TF", 
				"17560002104075TF-2", "17560002104076FT", "17560002104071TF", "17560000139450FT", "17560001127218FT", "17560003136984FT", 
				"17560003136992FT", "17560003136993FT", "17560003136990FT", "17560003136983FT", "17560001127172FT", "17560000590747FT", 
				"17560001126891FT", "17560001133472FT", "17560001127265FT", "17560000660429FT", "17560000143004FT", "17560000143005FT", 
				"17560001133285FT", "17560001133256FT", "17560001126616FT", "17560001353887FT", "17560001353888FT", "17560001121504FT", 
				"17560001130866FT", "L01"};
	
	public static String[] highwayFromZurichCamping = new String[] {"17560001849338FT", "17560001145319FT", "17560002072152FT",
				"17560001368750FT", "17560001368742TF", "17560001368690TF", "17560002155181TF", "17560001593985TF", "17560002018944FT",
				"17560002155154FT", "17560002227876FT", "17560001368847FT", "17560001368841FT", "17560000140446FT", "17560000140448FT",
				"17560001145319FT", "17560001145320FT", "17560000140414FT", "17560000140415FT", "17560001145319FT", "17560002214965FT",
				"17560000140441FT", "17560001774946TF", "17560001145319FT", "17560001145319FT", "17560000140044FT", "17560001145319FT",
				"17560001145319FT", "17560000140012FT", "17560001145319FT", "17560000143094FT", "17560001145699FT", "17560001145706FT",
				"17560000140746FT", "17560001143981FT", "17560000140701FT", "LP50a"};
	
//	#P01
	public static String[] toParking01 = new String[] {"LP01a"};

//	#P02
	public static String[] toParking02 = new String[] {"LP02a"};

//	#P03
	public static String[] toParking03 = new String[] {"17560001127985FT", "L17", "LP03a"};

//	#P04
	public static String[] toParking04 = new String[] {"17560001127985FT", "LP04a"};

//	#P05
	public static String[] toParking05 = new String[] {"17560001127985FT", "L17", "LP05a"};
	
//	#P06
	public static String[] toParking06 = new String[] {"L03", "L05", "L07", "LP06a"}; 
	
//	#P07
	public static String[] toParking07 = new String[] {"L03", "L05", "LP07a"};
	
//	#P08
	public static String[] toParking08 = new String[] {"L09", "L11", "L21", "LP08a"};
	
//	#P09
	public static String[] toParking09 = new String[] {"L09", "L11", "L13", "L25", "LP09a"};

//	24 entries - one for each hour
	public static int[] from1Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from2Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from3Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from4Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from5Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	private List<Id> routeFrom1ToParkings;
	private List<Id> routeFrom2ToParkings;
	private List<Id> routeFrom3ToParkings;
	private List<Id> routeFrom4ToParkings;
	private List<Id> routeFrom5ToParkings;
	
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
		createPopulation(scenario);
		log.info("done.");
		
		log.info("writing population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(populationFile);
		log.info("done.");
	}
	
	private void createRoutes(Scenario scenario) {

		routeFrom1ToParkings = new ArrayList<Id>();
		for (String id : from1) routeFrom1ToParkings.add(scenario.createId(id));
		for (String id : highwayFromZurich) routeFrom1ToParkings.add(scenario.createId(id));

		routeFrom2ToParkings = new ArrayList<Id>();
		for (String id : from2) routeFrom2ToParkings.add(scenario.createId(id));
		for (String id : highwayFromZurich) routeFrom2ToParkings.add(scenario.createId(id));

		routeFrom3ToParkings = new ArrayList<Id>();
		for (String id : from3) routeFrom3ToParkings.add(scenario.createId(id));
		for (String id : highwayFromBern) routeFrom3ToParkings.add(scenario.createId(id));
		
		routeFrom4ToParkings = new ArrayList<Id>();
		for (String id : from4) routeFrom4ToParkings.add(scenario.createId(id));
		for (String id : highwayFromBern) routeFrom4ToParkings.add(scenario.createId(id));
		
		routeFrom5ToParkings = new ArrayList<Id>();
		for (String id : from5) routeFrom5ToParkings.add(scenario.createId(id));
		for (String id : highwayFromBern) routeFrom5ToParkings.add(scenario.createId(id));
	}
	
	private void createPopulation(Scenario scenario) {
		
		createRoutePopulation(scenario, 1, from1Departures, routeFrom1ToParkings);
		createRoutePopulation(scenario, 2, from2Departures, routeFrom2ToParkings);
		createRoutePopulation(scenario, 3, from3Departures, routeFrom3ToParkings);
		createRoutePopulation(scenario, 4, from4Departures, routeFrom4ToParkings);
		createRoutePopulation(scenario, 5, from5Departures, routeFrom5ToParkings);
	}
	
	private void createRoutePopulation(Scenario scenario, int from, int[] fromDepartures, List<Id> routeFromToParkings) {
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		
		int hour = 1;
		Id fromLinkId = routeFromToParkings.get(0);
		Id toLinkId = routeFromToParkings.get(routeFromToParkings.size() - 1);
		Route route = routeFactory.createRoute(TransportMode.car, fromLinkId, toLinkId);
		((NetworkRoute) route).setLinkIds(fromLinkId, routeFromToParkings.subList(1, routeFromToParkings.size() - 1), toLinkId);		
		for (int departures : fromDepartures) {
			for (int hourCounter = 0; hourCounter < departures; hourCounter++) {
				Person person = populationFactory.createPerson(scenario.createId(visitorCounter + "_" + hourCounter + "_" + from + "_" + hour));
				
				Plan plan = populationFactory.createPlan();
				double departureTime = (hour - 1) * 3600 + Math.round(MatsimRandom.getRandom().nextDouble() * 3600.0);
				
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
				
			hour++;
			visitorCounter++;
		}
	}
}
