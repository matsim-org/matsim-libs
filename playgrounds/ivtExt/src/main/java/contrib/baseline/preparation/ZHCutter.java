/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package contrib.baseline.preparation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.strategies.ReRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.*;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.households.*;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.*;

import com.google.inject.Key;
import com.google.inject.name.Names;

import contrib.baseline.lib.F2LCreator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static contrib.baseline.preparation.IVTConfigCreator.*;

/**
 * Cuts an IVT baseline scenario to a predefined area.
 *
 * @author boescpa
 */
public class ZHCutter {

	private Map<Coord, Boolean> coordCache = new HashMap<>();
	private Map<Id<Person>, Person> filteredAgents = new HashMap<>();
	private Coord center;
	private int radius;
	private String commuterTag;
	private Scenario scenario;
	
	final String pathToInputScenarioFolder;

	private ZHCutter(ZHCutterConfigGroup cutterConfig) {
		this.coordCache = new HashMap<>();
		this.filteredAgents = new HashMap<>();
		pathToInputScenarioFolder = cutterConfig.getPathToInputScenarioFolder() + File.separator;
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(pathToInputScenarioFolder + PreparationScript.CONFIG));
		new PopulationReader(scenario).readFile(pathToInputScenarioFolder + POPULATION);
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(pathToInputScenarioFolder + POPULATION_ATTRIBUTES);
		new HouseholdsReaderV10(scenario.getHouseholds()).readFile(pathToInputScenarioFolder + HOUSEHOLDS);
		new ObjectAttributesXmlReader(scenario.getHouseholds().getHouseholdAttributes()).readFile(pathToInputScenarioFolder + HOUSEHOLD_ATTRIBUTES);
		new FacilitiesReaderMatsimV1(scenario).readFile(pathToInputScenarioFolder + FACILITIES);
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(pathToInputScenarioFolder + NETWORK);
		new TransitScheduleReader(scenario).readFile(pathToInputScenarioFolder + SCHEDULE);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(pathToInputScenarioFolder + VEHICLES);
		this.commuterTag = cutterConfig.getCommuterTag();
		this.center = new Coord(cutterConfig.getxCoordCenter(), cutterConfig.getyCoordCenter());
		this.radius = cutterConfig.getRadius();
	}
	
	public static void main(final String[] args) {		
		final Config config = ConfigUtils.loadConfig(args[0], new ZHCutterConfigGroup(ZHCutterConfigGroup.GROUP_NAME));
		final ZHCutterConfigGroup cutterConfig = (ZHCutterConfigGroup) config.getModule(ZHCutterConfigGroup.GROUP_NAME);
		// For 30km around Zurich Center (Bellevue): X - 2683518.0, Y - 1246836.0, radius - 30000

		// load files
		ZHCutter cutter = new ZHCutter(cutterConfig);
		// cut to area
		cutter.findInitialRoutes();
		Population filteredPopulation = cutter.geographicallyFilterPopulation();
		Households filteredHouseholds = cutter.filterHouseholdsWithPopulation();
		ActivityFacilities filteredFacilities = cutter.filterFacilitiesWithPopulation();
		TransitSchedule filteredSchedule = cutter.cutPT();
		Vehicles filteredVehicles = cutter.cleanVehicles(filteredSchedule);
		Network filteredOnlyCarNetwork = cutter.getOnlyCarNetwork(cutterConfig.getPathToTargetFolder());
		Network filteredNetwork = cutter.cutNetwork(filteredSchedule, filteredOnlyCarNetwork);
		cutter.resetPopulation(filteredPopulation);
		// write new files
		F2LCreator.createF2L(filteredFacilities, filteredOnlyCarNetwork, cutterConfig.getPathToTargetFolder() + File.separator + FACILITIES2LINKS);
		writeNewFiles(cutterConfig.getPathToTargetFolder() + File.separator, cutter.scenario,
				filteredPopulation, filteredHouseholds, filteredFacilities, filteredSchedule, filteredVehicles,
				filteredNetwork, cutter.createConfig(cutterConfig));
		cutter.cutPTCounts(filteredNetwork, cutterConfig);
	}

	private Config createConfig(ZHCutterConfigGroup cutterConfig) {
		Config config = ConfigUtils.createConfig();
		new IVTConfigCreator().makeConfigIVT(config, (int)(100*scenario.getConfig().qsim().getFlowCapFactor()));
		List<StrategyConfigGroup.StrategySettings> strategySettings = new ArrayList<>();
		strategySettings.add(getStrategySetting("ChangeExpBeta", 0.5));
		strategySettings.add(getStrategySetting("ReRoute", 0.2));
		strategySettings.add(getStrategySetting("BlackListedTimeAllocationMutator", 0.1));
		strategySettings.add(getStrategySetting("org.matsim.contrib.locationchoice.BestReplyLocationChoicePlanStrategy", 0.1));
		for (StrategyConfigGroup.StrategySettings strategy : strategySettings) {
			strategy.setSubpopulation(cutterConfig.commuterTag);
			config.getModule(StrategyConfigGroup.GROUP_NAME).addParameterSet(strategy);
		}
		return config;
	}

	private void resetPopulation(Population filteredPopulation) {
		PopulationFactory factory = filteredPopulation.getFactory();
		for (Person person : filteredPopulation.getPersons().values()) {
			final Plan origPlan = person.getSelectedPlan();
			// clean person completely and add cleaned plan
			person.getPlans().clear();
			final Plan newPlan = factory.createPlan();
			person.addPlan(newPlan);
			// create cleaned plan
			boolean lastWasLeg = false;
			for (PlanElement planElement : origPlan.getPlanElements()) {
				if (planElement instanceof Activity) {
					final Activity oldActivity = (Activity) planElement;
					if (oldActivity.getType().equals("pt interaction")) {
						continue;
					}
					final Coord actCoord = new Coord(oldActivity.getCoord().getX(), oldActivity.getCoord().getY());
					final Activity activity = factory.createActivityFromCoord(oldActivity.getType(), actCoord);
					activity.setEndTime(oldActivity.getEndTime());
					activity.setMaximumDuration(oldActivity.getMaximumDuration());
					activity.setStartTime(oldActivity.getStartTime());
					if (oldActivity.getFacilityId() != null) {
						final Activity activityImpl = (Activity) activity;
						activityImpl.setFacilityId(Id.create(oldActivity.getFacilityId().toString(), ActivityFacility.class));
					}
					newPlan.addActivity(activity);
					lastWasLeg = false;
				} else if (planElement instanceof Leg && !lastWasLeg) {
					final Leg oldLeg = (Leg) planElement;
					if (oldLeg.getMode().equals("transit_walk")) {
						newPlan.addLeg(factory.createLeg("pt"));
					} else {
						newPlan.addLeg(factory.createLeg(oldLeg.getMode()));
					}
					lastWasLeg = true;
				}
			}
		}
	}

	private void cutPTCounts(Network filteredNetwork, ZHCutterConfigGroup cutterConfig) {
		// ptStationCounts are manually cut...
		final String fileName = File.separator + "ptLinkCountsIdentified.csv";
		BufferedReader reader = IOUtils.getBufferedReader(cutterConfig.getPathToInputScenarioFolder() + fileName);
		List<String> countsToKeep = new LinkedList<>();
		BufferedWriter writer = IOUtils.getBufferedWriter(cutterConfig.getPathToTargetFolder() + fileName);
		try {
			countsToKeep.add(reader.readLine()); // header
			String line = reader.readLine();
			while (line != null) {
				String[] lineElements = line.split(";");
				Id<Link> linkId = Id.createLinkId(lineElements[0].trim());
				if (filteredNetwork.getLinks().containsKey(linkId)
						&& CoordUtils.calcEuclideanDistance(center, filteredNetwork.getLinks().get(linkId).getCoord()) <= radius - 5000) {
							// we keep only counts well (5km) within the area (else border effects to strong)
					countsToKeep.add(line);
				}
				line = reader.readLine();
			}
			reader.close();
			for (String lineToWrite : countsToKeep) {
				writer.write(lineToWrite);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Network getOnlyCarNetwork(String pathToTargetFolder) {
		Network carNetworkToKeep = NetworkUtils.createNetwork();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains("car") && (link.getCapacity() >= 1000 || // we keep all arterial links
					(CoordUtils.calcEuclideanDistance(center, link.getCoord()) <= radius + 5000))) { // and we keep all links within radius + 5km)
				addLink(carNetworkToKeep, link);
			}
		}
		new NetworkCleaner().run(carNetworkToKeep);
		new NetworkWriter(carNetworkToKeep).write(pathToTargetFolder + NETWORK);
		Network carNetworkToKeepCleaned = NetworkUtils.createNetwork();
		new NetworkReaderMatsimV1(carNetworkToKeepCleaned).readFile(pathToTargetFolder + NETWORK);
		new NetworkCleaner().run(carNetworkToKeepCleaned);
		new NetworkWriter(carNetworkToKeepCleaned).write(pathToTargetFolder + NETWORK);
		Network carNetworkToKeepFullyCleaned = NetworkUtils.createNetwork();
		new NetworkReaderMatsimV1(carNetworkToKeepFullyCleaned).readFile(pathToTargetFolder + NETWORK);
		return carNetworkToKeepFullyCleaned;
	}

	private Network cutNetwork(TransitSchedule filteredSchedule, Network filteredOnlyCarNetwork) {
		Network filteredNetwork = NetworkUtils.createNetwork();
		Set<Id<Link>> linksToKeep = getPTLinksToKeep(filteredSchedule);
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (linksToKeep.contains(link.getId()) || // we keep all links we need for pt
					filteredOnlyCarNetwork.getLinks().containsKey(link.getId())) {
				addLink(filteredNetwork, link);
				if (!linksToKeep.contains(link.getId())) {
					Set<String> allowedModes = new HashSet<>();
					allowedModes.add("car");
					link.setAllowedModes(allowedModes);
				}
			}
		}
		return filteredNetwork;
	}

	private Set<Id<Link>> getPTLinksToKeep(TransitSchedule filteredSchedule) {
		Set<Id<Link>> linksToKeep = new HashSet<>();
		for (TransitLine transitLine : filteredSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				linksToKeep.add(transitRoute.getRoute().getStartLinkId());
				for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
					linksToKeep.add(linkId);
				}
				linksToKeep.add(transitRoute.getRoute().getEndLinkId());
			}
		}
		return linksToKeep;
	}

	private void addLink(Network network, Link link) {
		if (!network.getNodes().containsKey(link.getFromNode().getId())) {
			Node node = network.getFactory().createNode(link.getFromNode().getId(), link.getFromNode().getCoord());
			network.addNode(node);
		}
		if (!network.getNodes().containsKey(link.getToNode().getId())) {
			Node node = network.getFactory().createNode(link.getToNode().getId(), link.getToNode().getCoord());
			network.addNode(node);
		}
		network.addLink(link);
		link.setFromNode(network.getNodes().get(link.getFromNode().getId()));
		link.setToNode(network.getNodes().get(link.getToNode().getId()));
	}

	private TransitSchedule cutPT() {
		TransitSchedule filteredSchedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for (TransitRouteStop transitStop : transitRoute.getStops()) {
					// change so that all trains are kept in any case.
					if (inArea(transitStop.getStopFacility().getCoord()) || transitRoute.getTransportMode().equals("rail")) {
						Id<TransitLine> newLineId = addLine(filteredSchedule, transitLine);
						filteredSchedule.getTransitLines().get(newLineId).addRoute(transitRoute);
						addStopFacilities(filteredSchedule, transitRoute);
						break;
					}
				}
			}
		}
		return filteredSchedule;
	}

	private void addStopFacilities(TransitSchedule schedule, TransitRoute transitRoute) {
		for (TransitRouteStop newStop : transitRoute.getStops()) {
			if (!schedule.getFacilities().containsKey(newStop.getStopFacility().getId())) {
				schedule.addStopFacility(newStop.getStopFacility());
			}
		}
	}

	private Id<TransitLine> addLine(TransitSchedule schedule, TransitLine transitLine) {
		Id<TransitLine> newLineId = Id.create(transitLine.getId().toString(), TransitLine.class);
		if (!schedule.getTransitLines().containsKey(newLineId)) {
			TransitLine newLine = schedule.getFactory().createTransitLine(newLineId);
			schedule.addTransitLine(newLine);
			newLine.setName(transitLine.getName());
		}
		return newLineId;
	}

	private Vehicles cleanVehicles(TransitSchedule transitSchedule) {
		Vehicles filteredVehicles = VehicleUtils.createVehiclesContainer();
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					Vehicle vehicleToKeep = scenario.getTransitVehicles().getVehicles().get(departure.getVehicleId());
					if (!filteredVehicles.getVehicleTypes().containsValue(vehicleToKeep.getType())) {
						filteredVehicles.addVehicleType(vehicleToKeep.getType());
					}
					filteredVehicles.addVehicle(vehicleToKeep);
				}
			}
		}
		return filteredVehicles;
	}

	private static void writeNewFiles(String pathToTargetFolder, Scenario scenario, Population filteredPopulation,
									  Households filteredHouseholds, ActivityFacilities filteredFacilities,
									  TransitSchedule filteredSchedule, Vehicles filteredVehicles, Network filteredNetwork, Config subscenarioConfig) {
		new PopulationWriter(filteredPopulation).write(pathToTargetFolder + POPULATION);
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes())
				.writeFile(pathToTargetFolder + POPULATION_ATTRIBUTES);
		new HouseholdsWriterV10(filteredHouseholds).writeFile(pathToTargetFolder + HOUSEHOLDS);
		new ObjectAttributesXmlWriter(scenario.getHouseholds().getHouseholdAttributes())
				.writeFile(pathToTargetFolder + HOUSEHOLD_ATTRIBUTES);
		new FacilitiesWriter(filteredFacilities).write(pathToTargetFolder + FACILITIES);
		new TransitScheduleWriter(filteredSchedule).writeFile(pathToTargetFolder + SCHEDULE);
		new VehicleWriterV1(filteredVehicles).writeFile(pathToTargetFolder + VEHICLES);
		new NetworkWriter(filteredNetwork).write(pathToTargetFolder + NETWORK);
		new ConfigWriter(subscenarioConfig).write(pathToTargetFolder + PreparationScript.CONFIG);
	}
	
	@SuppressWarnings("deprecation")
	public void findInitialRoutes() {
		Counter counter = new Counter(" initial routing # ");
		
		Config config = ConfigUtils.createConfig();
		config.setParam("f2l", "inputF2LFile", pathToInputScenarioFolder + FACILITIES2LINKS);
		
        WorldConnectLocations wcl = new WorldConnectLocations(config);
        wcl.connectFacilitiesWithLinks(scenario.getActivityFacilities(), scenario.getNetwork());
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);
		
		PreProcessDijkstra preprocessDijkstra = new PreProcessDijkstra();
		preprocessDijkstra.run(scenario.getNetwork());
		
		LeastCostPathCalculator leastCostPathCalculator = new Dijkstra(scenario.getNetwork(), travelDisutility, travelTime, preprocessDijkstra);
		RoutingModule routingModule = new NetworkRoutingModule("car", scenario.getPopulation().getFactory(), scenario.getNetwork(), leastCostPathCalculator);
		
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			counter.incCounter();
			List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan(), routingModule.getStageActivityTypes());
			
			for (Trip trip : trips) {
				if (mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals("car")) {
					ActivityFacility origin = scenario.getActivityFacilities().getFacilities().get(trip.getOriginActivity().getFacilityId());
					ActivityFacility destination = scenario.getActivityFacilities().getFacilities().get(trip.getOriginActivity().getFacilityId());
				
					List<Leg> legs = trip.getLegsOnly();
					if (legs.size() > 1) throw new IllegalStateException();
					
					List<? extends PlanElement> result = routingModule.calcRoute(origin, destination, legs.get(0).getDepartureTime(), person);
					legs.get(0).setRoute(((Leg)result.get(0)).getRoute());
				}
			}
		}
	}

	private Population geographicallyFilterPopulation() {
		ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();
		Population filteredPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		Counter counter = new Counter(" person # ");
		boolean actInArea;
		boolean actNotInArea;
		for (Person p : scenario.getPopulation().getPersons().values()) {
			counter.incCounter();
			if (p.getSelectedPlan() != null) {
				actInArea = false; actNotInArea = false;
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (inArea(act.getCoord())) {
							actInArea = true;
						} else {
							actNotInArea = true;
						}
					}
				}
				if (actInArea) {
					filteredPopulation.addPerson(p);
					filteredAgents.put(p.getId(), p);
					if (actNotInArea) {
						if (personAttributes.getAttribute(p.getId().toString(), "subpopulation") == null) {
							personAttributes.putAttribute(p.getId().toString(), "subpopulation", commuterTag);
						}
					}
				} else if (checkForRouteIntersection(p.getSelectedPlan())) {
					filteredPopulation.addPerson(p);
					filteredAgents.put(p.getId(), p);
					if (personAttributes.getAttribute(p.getId().toString(), "subpopulation") == null) {
						personAttributes.putAttribute(p.getId().toString(), "subpopulation", commuterTag);
					}
				} else {
					personAttributes.removeAllAttributes(p.getId().toString());
				}
			}
		}
		return filteredPopulation;
	}

	private boolean checkForRouteIntersection(Plan selectedPlan) {
		boolean routeIntersection = false;
		for (PlanElement pe : selectedPlan.getPlanElements()) {
			if (pe instanceof Leg && ((Leg) pe).getMode().equals("car")) {
				NetworkRoute route = (NetworkRoute) ((Leg) pe).getRoute();
				
				for (Id<Link> linkId : route.getLinkIds()) {
					if (inArea(scenario.getNetwork().getLinks().get(linkId).getCoord())) {
						routeIntersection = true;
						break;
					}
				}
			}
			if (routeIntersection) {
				break;
			}
		}
		return routeIntersection;
	}

	private Households filterHouseholdsWithPopulation() {
		Households filteredHouseholds = new HouseholdsImpl();

		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			Set<Id<Person>> personIdsToRemove = new HashSet<>();
			for (Id<Person> personId : household.getMemberIds()) {
				if (!filteredAgents.keySet().contains(personId)) {
					personIdsToRemove.add(personId);
				}
			}
			for (Id<Person> personId : personIdsToRemove) {
				household.getMemberIds().remove(personId);
			}
			if (!household.getMemberIds().isEmpty()) {
				filteredHouseholds.getHouseholds().put(household.getId(), household);
			} else {
				scenario.getHouseholds().getHouseholdAttributes().removeAllAttributes(household.getId().toString());
			}
		}

		return filteredHouseholds;
	}

	private ActivityFacilities filterFacilitiesWithPopulation() {
		ActivityFacilities filteredFacilities = FacilitiesUtils.createActivityFacilities();

		for (Person person : filteredAgents.values()) {
			if (person.getSelectedPlan() != null) {
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (act.getFacilityId() != null && !filteredFacilities.getFacilities().containsKey(act.getFacilityId())) {
							filteredFacilities.addActivityFacility(scenario.getActivityFacilities().getFacilities().get(act.getFacilityId()));
						}
					}
				}
			}
		}

		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			if (!filteredFacilities.getFacilities().containsValue(facility)
					&& (facility.getActivityOptions().containsKey(IVTConfigCreator.LEISURE)
						|| facility.getActivityOptions().containsKey(IVTConfigCreator.SHOP))
					&& inArea(facility.getCoord())) {
				filteredFacilities.addActivityFacility(facility);
			}
		}

		return filteredFacilities;
	}

	private boolean inArea(Coord coord) {
		if (coordCache.containsKey(coord)) {
			return coordCache.get(coord);
		} else {
			boolean coordIsInArea = CoordUtils.calcEuclideanDistance(center, coord) <= radius;
			coordCache.put(coord, coordIsInArea);
			return coordIsInArea;
		}
	}

	public static class ZHCutterConfigGroup extends ReflectiveConfigGroup {

		public static final String GROUP_NAME = "ZHCutter";

		private String commuterTag = "outAct";
		private String pathToInputScnearioFolder;
		private String pathToTargetFolder;
		// For 30km around Zurich Center (Bellevue): X - 2683518.0, Y - 1246836.0, radius - 30000
		private double xCoordCenter = 2683518.0;
		private double yCoordCenter = 1246836.0;
		private int radius = 30000;

		public ZHCutterConfigGroup(String name) {
			super(name);
		}

		@StringGetter("commuterTag") public String getCommuterTag() { return commuterTag; }
		@StringSetter("commuterTag") public void setCommuterTag(String commuterTag) { this.commuterTag = commuterTag; }
		@StringGetter("inputScenarioFolder") public String getPathToInputScenarioFolder() { return pathToInputScnearioFolder; }
		@StringSetter("inputScenarioFolder") public void setPathToInputScenarioFolder(String pathToInputScenarioFolder) { this.pathToInputScnearioFolder = pathToInputScenarioFolder; }
		@StringGetter("outputFolder") public String getPathToTargetFolder() { return pathToTargetFolder; }
		@StringSetter("outputFolder") public void setPathToTargetFolder(String pathToTargetFolder) { this.pathToTargetFolder = pathToTargetFolder; }
		@StringGetter("xCoordCenter") public double getxCoordCenter() { return xCoordCenter; }
		@StringSetter("xCoordCenter") public void setxCoordCenter(double xCoordCenter) { this.xCoordCenter = xCoordCenter; }
		@StringGetter("yCoordCenter") public double getyCoordCenter() { return yCoordCenter; }
		@StringSetter("yCoordCenter") public void setyCoordCenter(double yCoordCenter) { this.yCoordCenter = yCoordCenter; }
		@StringGetter("radius") public int getRadius() { return radius; }
		@StringSetter("radius") public void setRadius(int radius) { this.radius = radius; }
	}
}
