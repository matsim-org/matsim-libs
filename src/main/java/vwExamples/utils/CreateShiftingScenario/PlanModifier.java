/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package vwExamples.utils.CreateShiftingScenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.PtConstants;
import org.opengis.feature.simple.SimpleFeature;

import vwExamples.utils.tripAnalyzer.ExperiencedTrip;

/**
 * @author saxer
 */
public class PlanModifier {

	// Shape File to check home and work locations of the agents
	Set<String> cityZones;
	Set<String> serviceAreaZones;

	Map<String, Geometry> cityZonesMap;
	Map<String, Geometry> serviceAreazonesMap;

	String cityZonesFile;
	String serviceAreaZonesFile;

	String plansFile;
	String networkFile;
	String modPlansFile;
//	StreamingPopulationWriter modifiedPopulationWriter;
	Scenario scenario;

	Collection<String> stages;
	StageActivityTypes blackList;

	Network network;
	SubTourValidator subTourValidator; // Defines the rule to calculate absolute number of trips or agents that might
										// be shifted
	SubTourValidator assignTourValidator; // Defines the rule assign trips and agents that might be shifted

	ShiftingScenario shitingScenario;

	PlanModifier(String cityZonesFile, String serviceAreaZonesFile, String plansFile, String modPlansFile,
			String networkFile) {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		this.modifiedPopulationWriter = new StreamingPopulationWriter();
		this.modPlansFile = modPlansFile;
		this.cityZonesFile = cityZonesFile;
		this.serviceAreaZonesFile = serviceAreaZonesFile;

		new PopulationReader(this.scenario).readFile(plansFile);
		new MatsimNetworkReader(this.scenario.getNetwork()).readFile(networkFile);

		this.network = scenario.getNetwork();

		cityZones = new HashSet<String>();
		cityZonesMap = new HashMap<String, Geometry>();

		serviceAreaZones = new HashSet<String>();
		serviceAreazonesMap = new HashMap<String, Geometry>();

		readCityShape(this.cityZonesFile, "NO");
		readServiceAreaShape(this.serviceAreaZonesFile, "NO");

//		modifiedPopulationWriter = new StreamingPopulationWriter();

		// Add staging acts for pt and drt
		stages = new ArrayList<String>();
		stages.add(PtConstants.TRANSIT_ACTIVITY_TYPE);
		stages.add(new DrtStageActivityType("drt").drtStageActivity);
		stages.add(parking.ParkingRouterNetworkRoutingModule.parkingStageActivityType);
		blackList = new StageActivityTypesImpl(stages);

		subTourValidator = new isHomeOfficeSubTourCandidate(network, cityZonesMap, serviceAreazonesMap);
		assignTourValidator = new assignHomeOfficeSubTour(network, cityZonesMap, serviceAreazonesMap);
		shitingScenario = new ShiftingScenario(0.06);

	}

	public static void main(String[] args) {

		PlanModifier planmodifier = new PlanModifier(
				"D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\shp\\Hannover_Stadtteile.shp",
				"D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\shp\\Real_Region_Hannover.shp",
				"D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\plans\\vw243_cadON_ptSpeedAdj.0.1.output_plans.xml.gz",
				"D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\plans\\vw243_cadON_ptSpeedAdj.0.1_homeOffice_InOut.output_plans.xml.gz",
				"D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\network\\network.xml.gz");
		planmodifier.count();
		planmodifier.assign();

	}

	public void count() {

		// modifiedPopulationWriter.startStreaming(modPlansFile);

		for (Person person : scenario.getPopulation().getPersons().values()) {

			PersonUtils.removeUnselectedPlans(person);
			Plan plan = person.getSelectedPlan();
			for (Subtour subTour : TripStructureUtils.getSubtours(plan, blackList)) {

				String subtourMode = getSubtourMode(subTour, plan);

				if (subTourValidator.isValidSubTour(subTour)) {
					shitingScenario.agentSet.add(person.getId());
					shitingScenario.totalSubtourCounter.increment();

					// System.out.println(person.getId());

					// System.out.println(person.getId());
					for (Trip trip : subTour.getTrips()) {
						// for (Leg l : trip.getLegsOnly()) {
						// l.setRoute(null);
						// l.setTravelTime(0.0);
						//
						// TripRouter.insertTrip(plan, trip.getOriginActivity(),
						// Collections.singletonList(PopulationUtils.createLeg(TransportMode.drt)),
						// trip.getDestinationActivity());
						// }
						if (shitingScenario.mode2TripCounter.containsKey(subtourMode)) {
							shitingScenario.mode2TripCounter.get(subtourMode).increment();

						} else {
							shitingScenario.mode2TripCounter.put(subtourMode, new MutableInt(1));

						}

					}

				}

				// PersonUtils.removeUnselectedPlans(person);
				// modifiedPopulationWriter.writePerson(person);
			}
		}

		System.out.println(shitingScenario.mode2TripCounter);

		// modifiedPopulationWriter.closeStreaming();
	}

	public void assign() {
		String shiftingType = shitingScenario.type;
		int assignedSubTours = 0;
		int toursToBeAssigned = 0;

		// Part for subtourConversion
		if (shiftingType.equals("subtourConversion")) {

			toursToBeAssigned = (int) (shitingScenario.subTourConversionRate
					* shitingScenario.totalSubtourCounter.doubleValue());

			// Convert set to list in order to select random elements
			ArrayList<Id<Person>> agentCandidateList = new ArrayList<Id<Person>>(shitingScenario.agentSet);

			while (true) {
				System.out.println(assignedSubTours + " out of " + toursToBeAssigned);

				if (assignedSubTours >= toursToBeAssigned) {
					break;
				}

				int randomAgentCandidateIdx = (int) (Math.random() * (agentCandidateList.size() - 1));
				Id<Person> personId = agentCandidateList.get(randomAgentCandidateIdx);

				// Select a random person from agentSet
				Plan plan = scenario.getPopulation().getPersons().get(personId).getSelectedPlan();

				boolean foundTour = false;
				for (Subtour subTour : TripStructureUtils.getSubtours(plan, blackList)) {

					if (assignTourValidator.isValidSubTour(subTour)) {

						for (Trip trip : subTour.getTrips()) {
							for (Leg l : trip.getLegsOnly()) {
								l.setRoute(null);
								l.setTravelTime(0.0);

								TripRouter.insertTrip(plan, trip.getOriginActivity(),
										Collections.singletonList(PopulationUtils.createLeg("stayHome")),
										trip.getDestinationActivity());
							}

						}
						foundTour = true;

					}
					assignedSubTours++;

					if (foundTour == false) {
						agentCandidateList.remove(randomAgentCandidateIdx);

					}

				}

			}

		}

		//Write population
//		for (Person person : scenario.getPopulation().getPersons().values()) {
//			modifiedPopulationWriter.writePerson(person);
//		}

		new PopulationWriter(scenario.getPopulation(), null).write(modPlansFile);
//		modifiedPopulationWriter.startStreaming(modPlansFile);
//		modifiedPopulationWriter.closeStreaming();

	}

	public String getSubtourMode(Subtour subTour, Plan plan) {

		// ToDo: Inefficient way to get the legMode. Reduce loops!
		String subtourMode = null;
		List<Trip> trips = subTour.getTrips();

		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();

		for (TripStructureUtils.Trip trip : trips) {
			final List<PlanElement> fullTrip = plan.getPlanElements().subList(
					plan.getPlanElements().indexOf(trip.getOriginActivity()) + 1,
					plan.getPlanElements().indexOf(trip.getDestinationActivity()));
			subtourMode = mainModeIdentifier.identifyMainMode(fullTrip);
			return subtourMode;
		}

		return subtourMode;

	}

	public void readCityShape(String shapeFile, String featureKeyInShapeFile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(featureKeyInShapeFile).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			cityZones.add(id);
			cityZonesMap.put(id, geometry);
		}
	}

	public void readServiceAreaShape(String shapeFile, String featureKeyInShapeFile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(featureKeyInShapeFile).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			serviceAreaZones.add(id);
			serviceAreazonesMap.put(id, geometry);
		}
	}

	// class isHomeOfficeSubTourCandidate implements SubTourValidator {
	//
	//
	// @Override
	// public boolean isValidSubTour(Subtour subTour) {
	// boolean subTourInServiceArea = subTourIsWithinServiceArea(subTour);
	// String chain = getSubtourActivityChain(subTour);
	// String requiredChain = "home-work-home";
	//
	// if ((isInboundCommuterTour(subTour) || isOutboundCommuterTour(subTour) ||
	// isWithinCommuterTour(subTour))
	// && subTourInServiceArea && chain.equals(requiredChain)) {
	// return true;
	// }
	//
	// else
	// return false;
	// }
	//
	// }

}
