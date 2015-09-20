/* *********************************************************************** *
 * project: org.matsim.*
 * CreateBackgroundPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.vehicles.Vehicle;

public class CreateBackgroundPopulation {

	private static final Logger log = Logger.getLogger(CreateBackgroundPopulation.class);
	
	/*
	 * startLinkIds, endLinkIds and agentsPerOrigin have to use identical indices
	 * 
	 * Ids 0 and 1 are located north of Burgdorf, 2, 3 and 4 south of Burgdorf.
	 */
	public static String[] startLinkIds = new String[]{"17560003127350FT", "17560002104492FT", "17560000116600TF",
		"17560001814298FT", "17560000130806FT"};
	public static String[] endLinkIds = new String[]{"17560001813181TF", "17560001457290TF", "17560001834585TF",
		"17560000126838FT", "17560002172193FT"};

	public static String startLinkIdBurgdorf = "17560000140836TF";
	public static String endLinkIdBurgdorf = "17560000140836FT";
	
	// from 1: 17560000270156FT
	// to 1: 17560000269853FT
	// from 2: 17560001622819FT
	// to 2: 17560002068622TF
	// from 3: old: 17560000122176TF -> new: 17560001247396TF
	// to 3: old: 17560000122178FT -> new: 17560001247396FT
	public static String[] intersectionStartLinks = new String[]{"17560000270156FT", "17560001622819FT", "17560001247396TF"};
	public static String[] intersectionEndLinks = new String[]{"17560000269853FT", "17560002068622TF", "17560001247396FT"};
	
	public static String[] intersection1InCounts = {"17560001357914FT", "17560001811776FT", "17560001383083TF"};
	public static String[] intersection1OutCounts = {"17560000270101FT", "17560001885464FT", "17560000150179FT"};
	public static String[] intersection2InCounts = {"17560001529881FT", "17560000131238TF", "17560002102698FT"};
	public static String[] intersection2OutCounts = {"17560001529979FT", "17560000149655FT", "17560000149266TF"};
	public static String[] intersection3InCounts = {"17560000131238TF", "17560000149655FT", "17560001834647FT"};
	public static String[] intersection3OutCounts = {"17560002080561TF", "17560000131238TF", "17560002102719FT"};
	
	public static String[][] intersectionInCounts = new String[][]{intersection1InCounts, intersection2InCounts, intersection3InCounts};
	public static String[][] intersectionOutCounts = new String[][]{intersection1OutCounts, intersection2OutCounts, intersection3OutCounts};
	
	/*
	 * At least some trips *have* to start or end in Burgdorf according to
	 * the counts information.
	 * deltaNorthToSouth = Astra182ToBurgdorf - Astra023FromBurgdorf 
	 * deltaSouthToNorth = Astra023ToBurgdorf - Astra182FromBurgdorf
	 */
	public static String Astra023ToBurgdorf = "17560001529979FT";
	public static String Astra023FromBurgdorf = "17560001529881FT";
	public static String Astra182ToBurgdorf = "17560000150179FT";
	public static String Astra182FromBurgdorf = "17560001383083TF";
	
	// in a first step a simple approach: time independent number of agents per hour
//	public static int[] agentsPerOrigin = new int[]{3500, 3500, 3500, 3500, 3500};
	public static int[] agentsPerOrigin = new int[]{6000, 6000, 6000, 6000, 6000};
	
	public static double scaleFactor = 0.10;
	
	public static String countsFile = "../../matsim/mysimulations/burgdorf/input/counts_burgdorf.xml";
//	public static String networkFile = "../../matsim/mysimulations/burgdorf/input/network.xml.gz";
	public static String networkFile = "../../matsim/mysimulations/burgdorf/input/network_burgdorf_cut.xml.gz";
//	public static String facilitiesFile = "";
	public static String populationFile = "../../matsim/mysimulations/burgdorf/input/plans_background.xml.gz";
	public static String routesFile = "../../matsim/mysimulations/burgdorf/input/routes_background.xml.gz";
	
	public static boolean printRoutes = false;
	private Map<String, Route> routes;
	
	public static void main(String[] args) {
		new CreateBackgroundPopulation();
	}
	
	public CreateBackgroundPopulation() {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.counts().setCountsFileName(countsFile);
//		config.facilities().setInputFile(facilitiesFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFile);
				
		if (startLinkIds.length != endLinkIds.length) {
			throw new RuntimeException("Expected the same number of start and end links!");
		}
		int numOrigins = startLinkIds.length;
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new TravelTimeAndDistanceBasedTravelDisutilityFactory().createTravelDisutility(travelTime,
				config.planCalcScore());
		HighwayTravelDisutility highwayTravelDisutility = new HighwayTravelDisutility(travelDisutility);
		
		// create routes
		LeastCostPathCalculator router = new FastDijkstraFactory().createPathCalculator(scenario.getNetwork(), 
				highwayTravelDisutility, travelTime);

		routes = new LinkedHashMap<String, Route>();
		
		Random random = MatsimRandom.getRandom();
		
		/*
		 * Create "random" population
		 */
		createRoutes(scenario, router, numOrigins);
		createBasicPopulation(scenario, random, numOrigins);
		
		/*
		 * Create additional population from/to Burgdorf
		 */
		createBurgdorfRoutes(scenario, router, numOrigins);
		createBurgdorfPopulation(scenario, random, numOrigins, counts);

		/*
		 * Create node correction population
		 * Check the number of incoming and outgoing counts at intersections.
		 * The difference between those values is created as additional population.
		 */
		createIntersectionRoutes(scenario, router, numOrigins);
		createIntersectionPopulation(scenario, random, numOrigins, counts);		
		
		/*
		 * Select a random plan for each agent. Doing so avoids that all agents start
		 * with the same plan from the same location. Otherwise Cadyts seems to get confused
		 * at some stations.
		 */
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Person p = person;
			p.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(p));
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(populationFile);
		
		/*
		 * Write all routes to a population. Doing so allows checking them for errors.
		 */
		scenario.getPopulation().getPersons().clear();
		createRoutePopulation(scenario);
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(routesFile);
	}
	
	private void printRoute(Scenario scenario, Route route, String fromLink, String toLink) {
		if (!printRoutes) return;
		
		log.info("Route from " + fromLink + " to " + toLink);
		log.info(route.getStartLinkId() + "\t" + scenario.getNetwork().getLinks().get(route.getStartLinkId()).getFreespeed());
		for (Id<Link> linkId : ((NetworkRoute) route).getLinkIds()) {
			log.info(linkId + "\t" + scenario.getNetwork().getLinks().get(linkId).getFreespeed());
		}
		log.info(route.getEndLinkId() + "\t" + scenario.getNetwork().getLinks().get(route.getEndLinkId()).getFreespeed());
		log.info("");
	}
	
	/*
	 * Create Routes from all start to all end links. Routes from and to Burgdorf are NOT created.
	 */
	private void createRoutes(Scenario scenario, LeastCostPathCalculator router, int numOrigins) {
		for (int i = 0; i < numOrigins; i++) {
			for (int j = 0; j < numOrigins; j++) {
				Route route;
				if (i == j) {
					Id<Link> fromLinkId = Id.create(startLinkIds[i], Link.class);
					Id<Link> toLinkId = fromLinkId;
					
					route = new LinkNetworkRouteFactory().createRoute(fromLinkId, toLinkId);
					route.setDistance(0.0);
					route.setTravelTime(0.0);
				} else {
					Id<Link> fromLinkId = Id.create(startLinkIds[i], Link.class);
					Id<Link> toLinkId = Id.create(endLinkIds[j], Link.class);
					Link fromLink = scenario.getNetwork().getLinks().get(fromLinkId);
					Link toLink = scenario.getNetwork().getLinks().get(toLinkId);
							
					Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), 0.0, null, null);
					route = new LinkNetworkRouteFactory().createRoute(fromLinkId, toLinkId);
					
					double distance = RouteUtils.calcDistance((NetworkRoute) route, scenario.getNetwork());
					route.setDistance(distance);
					route.setTravelTime(path.travelTime);

					((NetworkRoute) route).setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
				}
				
				routes.put(startLinkIds[i] + "_" + endLinkIds[j], route);
				
				printRoute(scenario, route, startLinkIds[i], endLinkIds[j]);
			}
		}
		
		// fix bad routes
		String routeString;
		String[] links;
		List<Id<Link>> linkIds;
		NetworkRoute networkRoute;
			
		routeString = "17560000130806FT_17560000126838FT";
		//17560000130806FT - start link 
		//17560000126838FT - end link  
		links = new String[]{"17560001813912FT", "17560001813913FT", "17560002100968FT", "17560000122288FT", "17560000122328FT",
				"17560001368986TF", "17560001786482TF", "17560001862499FT", "17560000125722FT", "17560001812124FT", "17560002102719FT",
				"17560001838515FT", "17560001984994FT", "17560001984995FT", "17560001812144FT", "17560001834645FT", "17560000126288FT",
				"17560002102725FT", "17560002102726FT", "17560001368914FT", "17560001368915FT", "17560001846187FT"};
		linkIds = new ArrayList<Id<Link>>();
		for (String link : links) linkIds.add(Id.create(link, Link.class));
		networkRoute = (NetworkRoute) routes.get(routeString);
		networkRoute.setLinkIds(networkRoute.getStartLinkId(), linkIds, networkRoute.getEndLinkId());
	}
	
	/*
	 * Create Routes from and to Burgdorf.
	 */
	private void createBurgdorfRoutes(Scenario scenario, LeastCostPathCalculator router, int numOrigins) {
		// create to Burgdorf routes
		for (int i = 0; i < numOrigins; i++) {
			Route route;
			Id<Link> fromLinkId = Id.create(startLinkIds[i], Link.class);
			Id<Link> toLinkId = Id.create(endLinkIdBurgdorf, Link.class);
			Link fromLink = scenario.getNetwork().getLinks().get(fromLinkId);
			Link toLink = scenario.getNetwork().getLinks().get(toLinkId);
			
			Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), 0.0, null, null);
			route = new LinkNetworkRouteFactory().createRoute(fromLinkId, toLinkId);
			
			double distance = RouteUtils.calcDistance((NetworkRoute) route, scenario.getNetwork());
			route.setDistance(distance);
			route.setTravelTime(path.travelTime);
			
			((NetworkRoute) route).setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			
			routes.put(startLinkIds[i] + "_" + endLinkIdBurgdorf, route);
			
			printRoute(scenario, route, startLinkIds[i], endLinkIdBurgdorf);
		}
	
		// create from Burgdorf routes
		for (int i = 0; i < numOrigins; i++) {
			Route route;
			Id<Link> fromLinkId = Id.create(startLinkIdBurgdorf, Link.class);
			Id<Link> toLinkId = Id.create(endLinkIds[i], Link.class);
			Link fromLink = scenario.getNetwork().getLinks().get(fromLinkId);
			Link toLink = scenario.getNetwork().getLinks().get(toLinkId);
			
			Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), 0.0, null, null);
			route = new LinkNetworkRouteFactory().createRoute(fromLinkId, toLinkId);
			
			double distance = RouteUtils.calcDistance((NetworkRoute) route, scenario.getNetwork());
			route.setDistance(distance);
			route.setTravelTime(path.travelTime);
			
			((NetworkRoute) route).setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			
			routes.put(startLinkIdBurgdorf + "_" + endLinkIds[i], route);
			
			printRoute(scenario, route, startLinkIdBurgdorf, endLinkIds[i]);
		}
	}
	
	private void createIntersectionRoutes(Scenario scenario, LeastCostPathCalculator router, int numOrigins) {
		
		Route route;
		Id<Link> fromLinkId;
		Id<Link> toLinkId;
		Link fromLink;
		Link toLink;

		int numIntersections = intersectionStartLinks.length;
		for (int i = 0; i < numIntersections; i++) {

			// from intersection to end links
			fromLinkId = Id.create(intersectionStartLinks[i], Link.class);
			fromLink = scenario.getNetwork().getLinks().get(fromLinkId);
			for (int j = 0; j < numOrigins; j++) {
				
				toLinkId = Id.create(endLinkIds[j], Link.class);
				toLink = scenario.getNetwork().getLinks().get(toLinkId);
				
				Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), 0.0, null, null);
				route = new LinkNetworkRouteFactory().createRoute(fromLinkId, toLinkId);
				
				double distance = RouteUtils.calcDistance((NetworkRoute) route, scenario.getNetwork());
				route.setDistance(distance);
				route.setTravelTime(path.travelTime);
				
				((NetworkRoute) route).setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
				
				routes.put(intersectionStartLinks[i] + "_" + endLinkIds[j], route);
				
				printRoute(scenario, route, intersectionStartLinks[i], endLinkIds[j]);
			}
			
			// from start links to intersection
			toLinkId = Id.create(intersectionEndLinks[i], Link.class);
			toLink = scenario.getNetwork().getLinks().get(toLinkId);
			for (int j = 0; j < numOrigins; j++) {
				
				fromLinkId = Id.create(startLinkIds[j], Link.class);
				fromLink = scenario.getNetwork().getLinks().get(fromLinkId);
				
				Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), 0.0, null, null);
				route = new LinkNetworkRouteFactory().createRoute(fromLinkId, toLinkId);
				
				double distance = RouteUtils.calcDistance((NetworkRoute) route, scenario.getNetwork());
				route.setDistance(distance);
				route.setTravelTime(path.travelTime);
				
				((NetworkRoute) route).setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
				
				routes.put(startLinkIds[j] + "_" + intersectionEndLinks[i], route);
				
				printRoute(scenario, route, startLinkIds[j], intersectionEndLinks[i]);
			}
		}

		// fix bad intersection routes
		String routeString;
		String[] links;
		List<Id<Link>> linkIds;
		NetworkRoute networkRoute;
		
		routeString = "17560000130806FT_17560001247396FT";
		//17560000130806FT - start link 
		//17560001247396FT - end link
		links = new String[]{"17560001813912FT", "17560001813913FT", "17560002100968FT", "17560000122288FT", "17560000122328FT",
				"17560001368986TF", "17560001786482TF", "17560001368985TF", "17560002101148TF", "17560002101147TF", "17560000122366TF",
				"17560000122386FT", "17560000122387FT", "17560000122400FT", "17560000122416FT", "17560001862503FT", "17560000122384FT",
				"17560000122625FT", "17560000122410FT", "17560000122622FT", "17560000122674TF", "17560000122657TF", "17560000127847TF",
				"17560001808872TF", "17560000124049TF", "17560001809109FT", "17560000127772FT", "17560000123464FT", "17560000128169FT"};
		linkIds = new ArrayList<Id<Link>>();
		for (String link : links) linkIds.add(Id.create(link, Link.class));
		networkRoute = (NetworkRoute) routes.get(routeString);
		networkRoute.setLinkIds(networkRoute.getStartLinkId(), linkIds, networkRoute.getEndLinkId());
		
		routeString = "17560001247396TF_17560000126838FT";
		//17560001247396TF - start link 
		//17560000126838FT - end link
		links = new String[]{"17560000127856FT", "17560000127544FT", "17560000127853FT", "17560000127891FT", "17560000127889FT",
				"17560001862519FT", "17560002231593FT", "17560000122388TF", "17560000122388TF-1", "17560000122393TF", "17560001862502TF",
				"17560000125722FT", "17560001812124FT", "17560002102719FT", "17560001838515FT", "17560001984994FT", "17560001984995FT",
				"17560001812144FT", "17560001834645FT", "17560000126288FT", "17560002102725FT", "17560002102726FT", "17560001368914FT",
				"17560001368915FT", "17560001846187FT"};
		linkIds = new ArrayList<Id<Link>>();
		for (String link : links) linkIds.add(Id.create(link, Link.class));
		networkRoute = (NetworkRoute) routes.get(routeString);
		networkRoute.setLinkIds(networkRoute.getStartLinkId(), linkIds, networkRoute.getEndLinkId());
		
		routeString = "17560001814298FT_17560001247396FT";
		//17560001814298FT - start link 
		//17560001247396FT - end link
		links = new String[]{"17560001368904FT", "17560001813063FT", "17560001813064FT", "17560001368906FT", "17560001368907FT",
				"17560000126294FT", "17560001812171FT", "17560001834647FT", "17560002102722FT", "17560000125997TF", "17560000122613FT",
				"17560000122383FT", "17560000122625FT", "17560000122410FT", "17560000122622FT", "17560000122674TF", "17560000122657TF",
				"17560000127847TF", "17560001808872TF", "17560000124049TF", "17560001809109FT", "17560000127772FT", "17560000123464FT",
				"17560000128169FT"};
		linkIds = new ArrayList<Id<Link>>();
		for (String link : links) linkIds.add(Id.create(link, Link.class));
		networkRoute = (NetworkRoute) routes.get(routeString);
		networkRoute.setLinkIds(networkRoute.getStartLinkId(), linkIds, networkRoute.getEndLinkId());
		 
	}
	
	private void createBasicPopulation(Scenario scenario, Random random, int numOrigins) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		for (int i = 0; i < numOrigins; i++) {
			for (int t = 0; t < 24; t++) {
				for (int pers = 0; pers < agentsPerOrigin[i] * scaleFactor; pers++) {
					Person person = populationFactory.createPerson(Id.create(i + "_" + t + "_" + pers, Person.class));
					double departureTime = t * 3600 + Math.round(random.nextDouble() * 3600.0);
					
					for (int j = 0; j < numOrigins; j++) {
						Plan plan = populationFactory.createPlan();
						Id<Link> fromLinkId = null;
						Id<Link> toLinkId = null;
						if (i == j) {
							fromLinkId = Id.create(startLinkIds[i], Link.class);
							toLinkId = fromLinkId;
						} else {
							fromLinkId = Id.create(startLinkIds[i], Link.class);
							toLinkId = Id.create(endLinkIds[j], Link.class);
						}

						Activity fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
						fromActivity.setEndTime(departureTime);
						
						Leg leg = populationFactory.createLeg(TransportMode.car);
						leg.setDepartureTime(departureTime);
						leg.setRoute(routes.get(startLinkIds[i] + "_" + endLinkIds[j]));
						
						Activity toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
						
						plan.addActivity(fromActivity);
						plan.addLeg(leg);
						plan.addActivity(toActivity);
						
						person.addPlan(plan);
					}
					scenario.getPopulation().addPerson(person);
				}
			}
		}
	}
	
	/*
	 * Create population from/to Burgdorf Population
	 */
	private void createBurgdorfPopulation(Scenario scenario, Random random, int numOrigins, Counts counts) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		Count toBurgdorf023 = counts.getCount(Id.create(Astra023ToBurgdorf, Link.class));
		Count toBurgdorf182 = counts.getCount(Id.create(Astra182ToBurgdorf, Link.class));
		Count fromBurgdorf023 = counts.getCount(Id.create(Astra023FromBurgdorf, Link.class));
		Count fromBurgdorf182 = counts.getCount(Id.create(Astra182FromBurgdorf, Link.class));

		double[] toNorth = new double[24];
		double[] fromNorth = new double[24];
		double[] toSouth = new double[24];
		double[] fromSouth = new double[24];
		
		// from North to South
		log.info("North to South");
		for (int i = 1; i <= 24; i++) {
			double delta = toBurgdorf182.getVolume(i).getValue() - fromBurgdorf023.getVolume(i).getValue();
			log.info("\thour " + i + " " + delta);
			
			// delta > 0.0: less vehicles on count station after Burgdorf. Therefore, some stop at Burgdorf.
			if (delta > 0.0) {
				fromNorth[i - 1] = delta;
				toSouth[i - 1] = 0.0;
			} else {
				fromNorth[i - 1] = 0.0;
				toSouth[i - 1] = -delta;				
			}
		}
		log.info("");
		
		// from South to North
		log.info("South to North");
		for (int i = 1; i <= 24; i++) {
			double delta = toBurgdorf023.getVolume(i).getValue() - fromBurgdorf182.getVolume(i).getValue();
			log.info("\thour " + i + " " + delta);
			
			if (delta > 0.0) {
				fromSouth[i - 1] = delta;
				toNorth[i - 1] = 0.0;
			} else {
				fromSouth[i - 1] = 0.0;
				toNorth[i - 1] = -delta;				
			}
		}
		log.info("");
				
		// create to Burgdorf population
		// from north to Burgdorf
		for (int t = 0; t < 24; t++) {
			for (int pers = 0; pers < Math.round(fromNorth[t] * scaleFactor); pers++) {
				
				Person person = populationFactory.createPerson(Id.create("NorthToBurgdorf_" + t + "_" + pers, Person.class));
				double departureTime = t * 3600 + Math.round(random.nextDouble() * 3600.0);
				
				// only origins 0 and 1 are located north of Burgdorf
				Id<Link> toLinkId = Id.create(endLinkIdBurgdorf, Link.class);
				for (int i = 0; i < 2; i++) {
					Plan plan = populationFactory.createPlan();
					Id<Link> fromLinkId = Id.create(startLinkIds[i], Link.class);
					
					Activity fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
					fromActivity.setEndTime(departureTime);
					
					Leg leg = populationFactory.createLeg(TransportMode.car);
					leg.setDepartureTime(departureTime);
					leg.setRoute(routes.get(startLinkIds[i] + "_" + endLinkIdBurgdorf));
					
					Activity toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
					
					plan.addActivity(fromActivity);
					plan.addLeg(leg);
					plan.addActivity(toActivity);
					
					person.addPlan(plan);
				}
				scenario.getPopulation().addPerson(person);
			}
		}
		
		// from south to Burgdorf
		for (int t = 0; t < 24; t++) {
			for (int pers = 0; pers < Math.round(fromSouth[t] * scaleFactor); pers++) {
				
				Person person = populationFactory.createPerson(Id.create("SouthToBurgdorf_" + t + "_" + pers, Person.class));
				double departureTime = t * 3600 + Math.round(random.nextDouble() * 3600.0);
				
				// only origins 2, 3 and 4 are located south of Burgdorf
				Id<Link> toLinkId = Id.create(endLinkIdBurgdorf, Link.class);
				for (int i = 2; i < 5; i++) {
					Plan plan = populationFactory.createPlan();
					Id<Link> fromLinkId = Id.create(startLinkIds[i], Link.class);
					
					Activity fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
					fromActivity.setEndTime(departureTime);
					
					Leg leg = populationFactory.createLeg(TransportMode.car);
					leg.setDepartureTime(departureTime);
					leg.setRoute(routes.get(startLinkIds[i] + "_" + endLinkIdBurgdorf));
					
					Activity toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
					
					plan.addActivity(fromActivity);
					plan.addLeg(leg);
					plan.addActivity(toActivity);
					
					person.addPlan(plan);
				}
				scenario.getPopulation().addPerson(person);
			}
		}
		
		// create from Burgdorf population
		// from Burgdorf to north
		for (int t = 0; t < 24; t++) {
			for (int pers = 0; pers < Math.round(toNorth[t] * scaleFactor); pers++) {
				
				Person person = populationFactory.createPerson(Id.create("BurgdorfToNorth_" + t + "_" + pers, Person.class));
				double departureTime = t * 3600 + Math.round(random.nextDouble() * 3600.0);
				
				// only origins 0 and 1 are located north of Burgdorf
				Id<Link> fromLinkId = Id.create(startLinkIdBurgdorf, Link.class);
				for (int i = 0; i < 2; i++) {
					Plan plan = populationFactory.createPlan();
					Id<Link> toLinkId = Id.create(endLinkIds[i], Link.class);
					
					Activity fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
					fromActivity.setEndTime(departureTime);
					
					Leg leg = populationFactory.createLeg(TransportMode.car);
					leg.setDepartureTime(departureTime);
					leg.setRoute(routes.get(startLinkIdBurgdorf + "_" + endLinkIds[i]));
					
					Activity toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
					
					plan.addActivity(fromActivity);
					plan.addLeg(leg);
					plan.addActivity(toActivity);
					
					person.addPlan(plan);
				}
				scenario.getPopulation().addPerson(person);
			}
		}
		
		// from Burgdorf to south
		for (int t = 0; t < 24; t++) {
			for (int pers = 0; pers < Math.round(toSouth[t] * scaleFactor); pers++) {
				
				Person person = populationFactory.createPerson(Id.create("BurgdorfToSouth_" + t + "_" + pers, Person.class));
				double departureTime = t * 3600 + Math.round(random.nextDouble() * 3600.0);
				
				// only origins 2, 3 and 4 are located south of Burgdorf
				Id<Link> fromLinkId = Id.create(startLinkIdBurgdorf, Link.class);
				for (int i = 2; i < 5; i++) {
					Plan plan = populationFactory.createPlan();
					Id<Link> toLinkId = Id.create(endLinkIds[i], Link.class);
					
					Activity fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
					fromActivity.setEndTime(departureTime);
					
					Leg leg = populationFactory.createLeg(TransportMode.car);
					leg.setDepartureTime(departureTime);
					leg.setRoute(routes.get(startLinkIdBurgdorf + "_" + endLinkIds[i]));
					
					Activity toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
					
					plan.addActivity(fromActivity);
					plan.addLeg(leg);
					plan.addActivity(toActivity);
					
					person.addPlan(plan);
				}
				scenario.getPopulation().addPerson(person);
			}
		}
	}
	
	private void createIntersectionPopulation(Scenario scenario, Random random, int numOrigins, Counts counts) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		int numIntersections = intersectionStartLinks.length;
		for (int i = 0; i < numIntersections; i++) {
			
			String[] inCountStrings = intersectionInCounts[i];
			String[] outCountStrings = intersectionOutCounts[i];
			
			List<Count> inCounts = new ArrayList<Count>();
			List<Count> outCounts = new ArrayList<Count>();
			
			for (String string : inCountStrings) inCounts.add(counts.getCount(Id.create(string, Link.class)));
			for (String string : outCountStrings) outCounts.add(counts.getCount(Id.create(string, Link.class)));
			
			for (int t = 0; t < 24; t++) {
				
				// calculate count delta
				double in = 0.0;
				double out = 0.0;
				for (Count count : inCounts) in += count.getVolume(t + 1).getValue();
				for (Count count : outCounts) out += count.getVolume(t + 1).getValue();
				
				
				// more in than out counts -> some people leave the highway at the intersection
				double delta = in - out;
				if (delta > 0.0) {
					for (int pers = 0; pers < delta * scaleFactor; pers++) {
						Person person = populationFactory.createPerson(Id.create("leaveAtIntersection_" + i + "_" + t + "_" + pers, Person.class));
						double departureTime = t * 3600 + Math.round(random.nextDouble() * 3600.0);
						
						for (int j = 0; j < numOrigins; j++) {
							Plan plan = populationFactory.createPlan();
							Id<Link> fromLinkId = Id.create(startLinkIds[j], Link.class);
							Id<Link> toLinkId = Id.create(intersectionEndLinks[i], Link.class);
							
							Activity fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
							fromActivity.setEndTime(departureTime);
							
							Leg leg = populationFactory.createLeg(TransportMode.car);
							leg.setDepartureTime(departureTime);
							leg.setRoute(routes.get(startLinkIds[j] + "_" + intersectionEndLinks[i]));
							
							Activity toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
							
							plan.addActivity(fromActivity);
							plan.addLeg(leg);
							plan.addActivity(toActivity);
							
							person.addPlan(plan);
						}
						scenario.getPopulation().addPerson(person);
					}
				}
				// less in than out counts -> some people enter the highway at the intersection
				else {
					for (int pers = 0; pers < delta * scaleFactor; pers++) {
						Person person = populationFactory.createPerson(Id.create("enterAtIntersection_" + i + "_" + t + "_" + pers, Person.class));
						double departureTime = t * 3600 + Math.round(random.nextDouble() * 3600.0);
						
						for (int j = 0; j < numOrigins; j++) {
							Plan plan = populationFactory.createPlan();
							Id<Link> fromLinkId = Id.create(intersectionStartLinks[i], Link.class);
							Id<Link> toLinkId = Id.create(endLinkIds[j], Link.class);
							
							Activity fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
							fromActivity.setEndTime(departureTime);
							
							Leg leg = populationFactory.createLeg(TransportMode.car);
							leg.setDepartureTime(departureTime);
							leg.setRoute(routes.get(intersectionStartLinks[i] + "_" + endLinkIds[j]));
							
							Activity toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
							
							plan.addActivity(fromActivity);
							plan.addLeg(leg);
							plan.addActivity(toActivity);
							
							person.addPlan(plan);
						}
						scenario.getPopulation().addPerson(person);
					}
				}
		
			}				
		}
	}
	
	private void createRoutePopulation(Scenario scenario) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		int i = 0;
		
		for (Entry<String, Route> entry : this.routes.entrySet()) {
			Id<Person> personId = Id.create(entry.getKey(), Person.class);
			Route route = entry.getValue();
			
			Person person = populationFactory.createPerson(personId);

			Plan plan = populationFactory.createPlan();
			Id fromLinkId = route.getStartLinkId();
			Id toLinkId = route.getEndLinkId();

			double departureTime = i * 3600;
			
			Activity fromActivity = populationFactory.createActivityFromLinkId("tta", fromLinkId);
			fromActivity.setEndTime(departureTime);
			
			Leg leg = populationFactory.createLeg(TransportMode.car);
			leg.setDepartureTime(departureTime);
			leg.setRoute(route);
			
			Activity toActivity = populationFactory.createActivityFromLinkId("tta", toLinkId);
			
			plan.addActivity(fromActivity);
			plan.addLeg(leg);
			plan.addActivity(toActivity);
			
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
			
			i++;
		}
	}
	
	private static class HighwayTravelDisutility implements TravelDisutility {

		private final TravelDisutility travelDisutility;
		
		public HighwayTravelDisutility(TravelDisutility travelDisutility) {
			this.travelDisutility = travelDisutility;
		}
		
		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			double disutility = travelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
			
			double speed = link.getFreespeed() * 3.6;
			
			if (speed < 120.0) disutility *= 10;
			if (speed < 100.0) disutility *= 10;
			if (speed < 80.0) disutility *= 10;
			
			return disutility;
			
//			if (speed < (100.0/3.6)) return disutility * 1000.0;
//			else return disutility;
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			double disutility = travelDisutility.getLinkMinimumTravelDisutility(link);
			
//			if (link.getFreespeed() < (100.0/3.6)) return disutility * 1000.0;
//			else return disutility;
			
			double speed = link.getFreespeed() * 3.6;
			
			if (speed < 120.0) disutility *= 10;
			if (speed < 100.0) disutility *= 10;
			if (speed < 80.0) disutility *= 10;
			
			return disutility;
		}
		
	}
}