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
	
//	#3
	public static String[] from3 = new String[]{"17560000116600TF", "17560001985055TF", "17560000118607FT", "17560001985071FT", 
				"17560000118970FT", "17560002102697FT", "17560002102698FT", "17560001985497TF", "17560000149269FT", "17560001840179FT",
				"17560002102704FT", "17560002014557FT", "17560001813096TF", "17560001529979FT", "17560000142885FT", "17560000142879FT", 
				"17560001368738FT", "17560001849337FT"}; 
//	#4
	public static String[] from4 = new String[]{"17560001814298FT", "17560001368904FT", "17560001813063FT", "17560001813064FT",
				"17560001368906FT", "17560001368907FT", "17560000126294FT", "17560001812171FT", "17560001834647FT", "17560002102722FT",
				"17560001813075FT", "17560001812069TF", "17560001853018TF", "17560000131237TF", "17560000131238TF", "17560000149624FT", 
				"17560001813087FT", "17560001813096TF", "17560001529979FT", "17560000142885FT", "17560000142879FT", "17560001368738FT", 
				"17560001849337FT"};
	
//	#5
	public static String[] from5 = new String[]{"17560000130806FT", "17560001813912FT", "17560001813913FT", "17560002100968FT", 
				"17560000122288FT", "17560000122328FT", "17560001368986TF", "17560000122408TF", "17560000122648TF", "17560002077641TF",
				"17560001812069TF", "17560001853018TF", "17560000131237TF", "17560000131238TF", "17560000149624FT", "17560001813087FT", 
				"17560001813096TF", "17560001529979FT", "17560000142885FT", "17560000142879FT", "17560001368738FT", "17560001849337FT"};
	
	public static String[] highwayFromBern = new String[] {	"17560001849338FT", "17560001368756FT", "17560002104075TF", 
				"17560002104075TF-2", "17560002104076FT", "17560002104071TF", "17560000139450FT", "17560001127218FT", "17560003136984FT", 
				"17560003136992FT", "17560003136993FT", "17560003136990FT", "17560003136983FT", "17560001127172FT", "17560000590747FT", 
				"17560001126891FT", "17560001133472FT", "17560001127265FT", "17560000660429FT", "17560000143004FT", "17560000143005FT", 
				"17560001133285FT", "17560001133256FT", "17560001126616FT", "17560001353887FT", "17560001353888FT", "17560001121504FT", 
				"17560001130866FT", "L01"};
	
//	#P07
	public static String[] toParking07 = new String[] {"L03", "L05", "LP07a"}; 
	
//	#P08
	public static String[] toParking08 = new String[] {"L09", "L11", "L21", "LP08a"};
	
//	#P09
	public static String[] toParking09 = new String[] {"L09", "L11", "L13", "L25", "LP09a"};

	// 24 entries - one for each hour
	public static int[] from3Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from4Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static int[] from5Departures = new int[]{ 0, 0, 0, 0, 0, 100, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
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
		
		createRoutePopulation(scenario, 3, routeFrom3ToParkings);
		createRoutePopulation(scenario, 4, routeFrom4ToParkings);
		createRoutePopulation(scenario, 5, routeFrom5ToParkings);
	}
	
	private void createRoutePopulation(Scenario scenario, int from, List<Id> routeFromToParkings) {
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();

		int hour = 1;
		Id fromLinkId = routeFromToParkings.get(0);
		Id toLinkId = routeFromToParkings.get(routeFromToParkings.size() - 1);
		Route route = routeFactory.createRoute(TransportMode.car, fromLinkId, toLinkId);
		((NetworkRoute) route).setLinkIds(fromLinkId, routeFromToParkings.subList(1, routeFromToParkings.size() - 1), toLinkId);		
		for (int departures : from3Departures) {
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
