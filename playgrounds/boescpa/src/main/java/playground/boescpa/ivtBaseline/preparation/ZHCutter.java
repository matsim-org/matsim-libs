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

package playground.boescpa.ivtBaseline.preparation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.*;
import org.matsim.households.*;
import org.matsim.pt.transitSchedule.TransitLineImpl;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.*;

import java.io.File;
import java.util.*;

import static playground.boescpa.ivtBaseline.preparation.IVTConfigCreator.*;

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

	/*protected static final String FACILITIES = File.separator + IVTConfigCreator.FACILITIES;
	protected static final String HOUSEHOLD_ATTRIBUTES = File.separator + IVTConfigCreator.HOUSEHOLD_ATTRIBUTES;
	protected static final String HOUSEHOLDS = File.separator + IVTConfigCreator.HOUSEHOLDS;
	protected static final String POPULATION = File.separator + IVTConfigCreator.POPULATION;
	protected static final String POPULATION_ATTRIBUTES = File.separator + IVTConfigCreator.POPULATION_ATTRIBUTES;
	protected static final String NETWORK = File.separator + IVTConfigCreator.NETWORK;*/

	public ZHCutter() {
		this.reset();
	}

	public static void main(final String[] args) {
		/*final String pathToFolder = args[0];
		final String pathToTargetFolder = args[1];
		final double xCoordCenter = Double.parseDouble(args[2]);
		final double yCoordCenter = Double.parseDouble(args[3]);
		final int radius = Integer.parseInt(args[4]);
		// For 30km around Zurich Center (Bellevue): X - 2683518.0, Y - 1246836.0, radius - 30000

		// load files
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReaderMatsimV5(scenario).readFile(pathToFolder + POPULATION);
		ObjectAttributes personAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(personAttributes).parse(pathToFolder + POPULATION_ATTRIBUTES);
		Households origHouseholds = new HouseholdsImpl();
		new HouseholdsReaderV10(origHouseholds).readFile(pathToFolder + HOUSEHOLDS);
		ObjectAttributes householdAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdAttributes).parse(pathToFolder + HOUSEHOLD_ATTRIBUTES);
		new FacilitiesReaderMatsimV1(scenario).readFile(pathToFolder + FACILITIES);
		new NetworkReaderMatsimV1(scenario.getNetwork()).parse(pathToFolder + NETWORK);*/

		final Config config = ConfigUtils.loadConfig(args[0], new ZHCutterConfigGroup(ZHCutterConfigGroup.GROUP_NAME));
		final ZHCutterConfigGroup cutterConfig = (ZHCutterConfigGroup) config.getModule(ZHCutterConfigGroup.GROUP_NAME);
		// For 30km around Zurich Center (Bellevue): X - 2683518.0, Y - 1246836.0, radius - 30000

		// load files
		Scenario scenario = ScenarioUtils.createScenario(config);
		final String pathToInputScenarioFolder = cutterConfig.getPathToInputScenarioFolder() + File.separator;
		/*new PopulationReaderMatsimV5(scenario).readFile(cutterConfig.getPopulation());
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).parse(cutterConfig.getPopulationAttributes());
		new HouseholdsReaderV10(scenario.getHouseholds()).readFile(cutterConfig.getHouseholds());
		new ObjectAttributesXmlReader(scenario.getHouseholds().getHouseholdAttributes()).parse(cutterConfig.getHouseholdAttributes());
		new FacilitiesReaderMatsimV1(scenario).readFile(cutterConfig.getFacilities());
		new NetworkReaderMatsimV1(scenario.getNetwork()).parse(cutterConfig.getNetwork());*/
		new PopulationReaderMatsimV5(scenario).readFile(pathToInputScenarioFolder + POPULATION);
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).parse(pathToInputScenarioFolder + POPULATION_ATTRIBUTES);
		new HouseholdsReaderV10(scenario.getHouseholds()).readFile(pathToInputScenarioFolder + HOUSEHOLDS);
		new ObjectAttributesXmlReader(scenario.getHouseholds().getHouseholdAttributes()).parse(pathToInputScenarioFolder + HOUSEHOLD_ATTRIBUTES);
		new FacilitiesReaderMatsimV1(scenario).readFile(pathToInputScenarioFolder + FACILITIES);
		new NetworkReaderMatsimV1(scenario.getNetwork()).parse(pathToInputScenarioFolder + NETWORK);
		new TransitScheduleReader(scenario).readFile(pathToInputScenarioFolder + SCHEDULE);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(pathToInputScenarioFolder + VEHICLES);
		double xCoordCenter = cutterConfig.getxCoordCenter();
		double yCoordCenter = cutterConfig.getyCoordCenter();
		int radius = cutterConfig.getRadius();
		// cut to area
		ZHCutter cutter = new ZHCutter();
		cutter.commuterTag = cutterConfig.getCommuterTag();
		cutter.setArea(new Coord(xCoordCenter, yCoordCenter), radius);
		Population filteredPopulation = cutter.geographicallyFilterPopulation(scenario.getPopulation(), scenario.getNetwork());
		Households filteredHouseholds = cutter.filterHouseholdsWithPopulation(scenario.getHouseholds());
		ActivityFacilities filteredFacilities = cutter.filterFacilitiesWithPopulation(scenario.getActivityFacilities());
		TransitSchedule filteredSchedule = cutter.cutPT(scenario.getTransitSchedule());
		Vehicles filteredVehicles = cutter.cleanVehicles(filteredSchedule, scenario.getTransitVehicles());
		// write new files
		writeNewFiles(cutterConfig.getPathToTargetFolder() + File.separator, scenario,
				filteredPopulation, filteredHouseholds, filteredFacilities, filteredSchedule, filteredVehicles);
	}

	private TransitSchedule cutPT(TransitSchedule transitSchedule) {
		TransitSchedule filteredSchedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for (TransitRouteStop transitStop : transitRoute.getStops()) {
					if (inArea(transitStop.getStopFacility().getCoord())) {
						Id<TransitLine> newLineId = addLine(filteredSchedule, transitLine);
						filteredSchedule.getTransitLines().get(newLineId).addRoute(transitRoute);
						addStopFacilities(filteredSchedule, transitRoute);//, transitStop);
						break;
					}
				}
			}
		}
		return filteredSchedule;
	}

	private void addStopFacilities(TransitSchedule schedule, TransitRoute transitRoute) { //, TransitRouteStop transitStop) {
		for (TransitRouteStop newStop : transitRoute.getStops()) {
			/*Id<TransitStopFacility> newStopFacilityId =
					Id.create(transitStop.getStopFacility().getId().toString(), TransitStopFacility.class);
			if (!schedule.getFacilities().containsKey(newStopFacilityId)) {*/
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

	/*private void cleanStops(TransitSchedule transitSchedule) {
		Set<Id<TransitStopFacility>> facilitiesToKeep = new HashSet<>();
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					facilitiesToKeep.add(stop.getStopFacility().getId());
				}
			}
		}
		List<Id<TransitStopFacility>> facilitiesToRemove = new LinkedList<>();
		for (Id<TransitStopFacility> stopFacilityId : transitSchedule.getFacilities().keySet()) {
			if (!facilitiesToKeep.contains(stopFacilityId)) {
				facilitiesToRemove.add(stopFacilityId);
			}
		}
		for (Id<TransitStopFacility> facilityToRemove : facilitiesToRemove) {
			transitSchedule.getFacilities().remove(facilityToRemove);
		}
	}*/

	private Vehicles cleanVehicles(TransitSchedule transitSchedule, Vehicles transitVehicles) {
		Vehicles filteredVehicles = VehicleUtils.createVehiclesContainer();
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					Vehicle vehicleToKeep = transitVehicles.getVehicles().get(departure.getVehicleId());
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
									  TransitSchedule filteredSchedule, Vehicles filteredVehicles) {
		new PopulationWriter(filteredPopulation).write(pathToTargetFolder + POPULATION);
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes())
				.writeFile(pathToTargetFolder + POPULATION_ATTRIBUTES);
		new HouseholdsWriterV10(filteredHouseholds).writeFile(pathToTargetFolder + HOUSEHOLDS);
		new ObjectAttributesXmlWriter(scenario.getHouseholds().getHouseholdAttributes())
				.writeFile(pathToTargetFolder + HOUSEHOLD_ATTRIBUTES);
		new FacilitiesWriter(filteredFacilities).writeV1(pathToTargetFolder + FACILITIES);
		new TransitScheduleWriter(filteredSchedule).writeFileV1(pathToTargetFolder + SCHEDULE);
		new VehicleWriterV1(filteredVehicles).writeFile(pathToTargetFolder + VEHICLES);
	}

	public void reset() {
		coordCache.clear();
		filteredAgents.clear();
	}

	public void setArea(Coord center, int radius) {
		this.center = center;
		this.radius = radius;
	}

	public Population geographicallyFilterPopulation(final Population origPopulation, Network network) {
		ObjectAttributes personAttributes = origPopulation.getPersonAttributes();
		Population filteredPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		Counter counter = new Counter(" person # ");
		boolean actInArea;
		boolean actNotInArea;
		for (Person p : origPopulation.getPersons().values()) {
			counter.incCounter();
			if (p.getSelectedPlan() != null) {
				actInArea = false; actNotInArea = false;
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;
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
						personAttributes.putAttribute(p.toString(), "subpopulation", commuterTag);
					}
				} else if (checkForRouteIntersection(network, p.getSelectedPlan())) {
					filteredPopulation.addPerson(p);
					filteredAgents.put(p.getId(), p);
					personAttributes.putAttribute(p.toString(), "subpopulation", commuterTag);
				}
				else {
					personAttributes.removeAllAttributes(p.toString());
				}
			}
		}
		return filteredPopulation;
	}

	private boolean checkForRouteIntersection(Network network, Plan selectedPlan) {
		boolean routeIntersection = false;
		for (PlanElement pe : selectedPlan.getPlanElements()) {
			if (pe instanceof Leg && ((Leg) pe).getMode().equals("car")) {
				NetworkRoute route = (NetworkRoute) ((Leg) pe).getRoute();
				for (Id<Link> linkId : route.getLinkIds()) {
					if (inArea(network.getLinks().get(linkId).getCoord())) {
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

	public Households filterHouseholdsWithPopulation(final Households households) {
		Households filteredHouseholds = new HouseholdsImpl();

		for (Household household : households.getHouseholds().values()) {
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
				households.getHouseholdAttributes().removeAllAttributes(household.getId().toString());
			}
		}

		return filteredHouseholds;
	}

	public ActivityFacilities filterFacilitiesWithPopulation(final ActivityFacilities origFacilities) {
		ActivityFacilities filteredFacilities = FacilitiesUtils.createActivityFacilities();

		for (Person person : filteredAgents.values()) {
			if (person.getSelectedPlan() != null) {
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;
						if (act.getFacilityId() != null && !filteredFacilities.getFacilities().containsKey(act.getFacilityId())) {
							filteredFacilities.addActivityFacility(origFacilities.getFacilities().get(act.getFacilityId()));
						}
					}
				}
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

	private static class ZHCutterConfigGroup extends ReflectiveConfigGroup {

		static final String GROUP_NAME = "ZHCutter";

		private String commuterTag = "outAct";
		/*private String facilities;
		private String householdAttributes;
		private String households;
		private String population;
		private String populationAttributes;
		private String network;*/
		private String pathToInputScnearioFolder;
		private String pathToTargetFolder;
		// For 30km around Zurich Center (Bellevue): X - 2683518.0, Y - 1246836.0, radius - 30000
		private double xCoordCenter = 2683518.0;
		private double yCoordCenter = 1246836.0;
		private int radius = 30000;

		ZHCutterConfigGroup(String name) {
			super(name);
		}

		@StringGetter("commuterTag") String getCommuterTag() { return commuterTag; }
		@StringSetter("commuterTag") void setCommuterTag(String commuterTag) { this.commuterTag = commuterTag; }
		/*@StringGetter("facilities") String getFacilities() { return facilities; }
		@StringSetter("facilities") void setFacilities(String facilities) { this.facilities = facilities; }
		@StringGetter("householdAttributes") String getHouseholdAttributes() { return householdAttributes; }
		@StringSetter("householdAttributes") void setHouseholdAttributes(String householdAttributes) { this.householdAttributes = householdAttributes; }
		@StringGetter("households") String getHouseholds() { return households; }
		@StringSetter("households") void setHouseholds(String households) { this.households = households; }
		@StringGetter("population") String getPopulation() { return population; }
		@StringSetter("population") void setPopulation(String population) { this.population = population; }
		@StringGetter("populationAttributes") String getPopulationAttributes() { return populationAttributes; }
		@StringSetter("populationAttributes") void setPopulationAttributes(String populationAttributes) { this.populationAttributes = populationAttributes; }
		@StringGetter("network") String getNetwork() { return network; }
		@StringSetter("network") void setNetwork(String network) { this.network = network; }*/
		@StringGetter("inputScenarioFolder") String getPathToInputScenarioFolder() { return pathToInputScnearioFolder; }
		@StringSetter("inputScenarioFolder") void setPathToInputScenarioFolder(String pathToInputScenarioFolder) { this.pathToInputScnearioFolder = pathToInputScenarioFolder; }
		@StringGetter("outputFolder") String getPathToTargetFolder() { return pathToTargetFolder; }
		@StringSetter("outputFolder") void setPathToTargetFolder(String pathToTargetFolder) { this.pathToTargetFolder = pathToTargetFolder; }
		@StringGetter("xCoordCenter") double getxCoordCenter() { return xCoordCenter; }
		@StringSetter("xCoordCenter") void setxCoordCenter(double xCoordCenter) { this.xCoordCenter = xCoordCenter; }
		@StringGetter("yCoordCenter") double getyCoordCenter() { return yCoordCenter; }
		@StringSetter("yCoordCenter") void setyCoordCenter(double yCoordCenter) { this.yCoordCenter = yCoordCenter; }
		@StringGetter("radius") int getRadius() { return radius; }
		@StringSetter("radius") void setRadius(int radius) { this.radius = radius; }
	}
}
