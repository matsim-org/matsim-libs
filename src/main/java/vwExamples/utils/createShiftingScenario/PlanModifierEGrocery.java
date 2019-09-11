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

package vwExamples.utils.createShiftingScenario;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.PtConstants;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author saxer
 */
public class PlanModifierEGrocery {

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
	// StreamingPopulationWriter modifiedPopulationWriter;
	Scenario scenario;

	Collection<String> stages;
	StageActivityTypes blackList;

	Network network;
	SubTourValidator subTourValidator; // Defines the rule to calculate absolute number of trips or agents that might
										// be shifted
	SubTourValidator assignTourValidator; // Defines the rule assign trips and agents that might be shifted

	ShiftingScenario shiftingScenario;
	String sep = ";";

	PlanModifierEGrocery(String cityZonesFile, String serviceAreaZonesFile, String plansFile, String modPlansFile,
			String networkFile) {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// this.modifiedPopulationWriter = new StreamingPopulationWriter();
		this.modPlansFile = modPlansFile;
		this.cityZonesFile = cityZonesFile;
		this.serviceAreaZonesFile = serviceAreaZonesFile;

		this.plansFile = plansFile;
		this.networkFile = networkFile;
		new PopulationReader(this.scenario).readFile(plansFile);
		new MatsimNetworkReader(this.scenario.getNetwork()).readFile(networkFile);

		this.network = scenario.getNetwork();

		cityZones = new HashSet<String>();
		cityZonesMap = new HashMap<String, Geometry>();

		serviceAreaZones = new HashSet<String>();
		serviceAreazonesMap = new HashMap<String, Geometry>();

		readCityShape(this.cityZonesFile, "NO");
		readServiceAreaShape(this.serviceAreaZonesFile, "NO");

		// modifiedPopulationWriter = new StreamingPopulationWriter();

		// Add staging acts for pt and drt
		stages = new ArrayList<String>();
		stages.add(PtConstants.TRANSIT_ACTIVITY_TYPE);
		stages.add(new DrtStageActivityType("drt").drtStageActivity);
		stages.add(parking.ParkingRouterNetworkRoutingModule.parkingStageActivityType);
		blackList = new StageActivityTypesImpl(stages);

		subTourValidator = new isShoppingSubTourCandidate(network, cityZonesMap, serviceAreazonesMap);
		assignTourValidator = new assignShoppingCandidate(network, cityZonesMap, serviceAreazonesMap);
		shiftingScenario = new ShiftingScenario(0.1);

	}

	public static void main(String[] args) {

		PlanModifierEGrocery planmodifier = new PlanModifierEGrocery(
				"D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Zellen\\Stadtteile\\Hannover_Stadtteile_25832.shp",
				"D:\\\\Thiel\\\\Programme\\\\WVModell\\\\00_Eingangsdaten\\\\Zellen\\\\Stadtteile\\\\Hannover_Stadtteile_25832.shp",
				"D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw243_cadON_ptSpeedAdj.0.1\\vw243_cadON_ptSpeedAdj.0.1.output_plans.xml.gz",
				"D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\vw243_0.1_EGrocery0.1\\vw243_0.1_EGrocery0.1_input.xml.gz",
				"D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\vw243_0.1\\network\\network_editedPt.xml.gz");
		planmodifier.count();
		planmodifier.assign();
		planmodifier.writeScenarioInformation();

	}

	public void writeScenarioInformation() {

		String outputFolder = new File(this.plansFile).getParent();

		try {

			BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder + "\\scenarioStats.csv");

			String header1 = "RequiredTours" + sep + "AssigendTours";
			bw.write(header1);
			bw.newLine();
			bw.write(shiftingScenario.toursToBeAssigned + sep + shiftingScenario.assignedSubTours);
			bw.newLine();
			bw.write("Available trips (counted)");
			bw.newLine();

			for (Entry<String, MutableInt> entry : shiftingScenario.mode2TripCounter.entrySet()) {
				String mode = entry.getKey();
				int tripCount = entry.getValue().getValue();

				String row = mode + sep + tripCount;
				bw.write(row);
				bw.newLine();
			}

			bw.write("Shifted trips (counted)");
			bw.newLine();

			for (Entry<String, MutableInt> entry : shiftingScenario.mode2ShiftedTripCounter.entrySet()) {
				String mode = entry.getKey();
				int tripCount = entry.getValue().getValue();

				String row = mode + sep + tripCount;
				bw.write(row);
				bw.newLine();
			}

			bw.newLine();

			bw.flush();
			bw.close();

		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("Could not write scenario statistics");
		}

	}

	public void count() {

		// modifiedPopulationWriter.startStreaming(modPlansFile);

		for (Person person : scenario.getPopulation().getPersons().values()) {

			PersonUtils.removeUnselectedPlans(person);
			Plan plan = person.getSelectedPlan();
			for (Subtour subTour : TripStructureUtils.getSubtours(plan, blackList)) {

				String subtourMode = getSubtourMode(subTour, plan);

				if (subTourValidator.isValidSubTour(subTour)) {
					shiftingScenario.agentSet.add(person.getId());
					shiftingScenario.totalSubtourCounter.increment();

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
						if (shiftingScenario.mode2TripCounter.containsKey(subtourMode)) {
							shiftingScenario.mode2TripCounter.get(subtourMode).increment();

						} else {
							shiftingScenario.mode2TripCounter.put(subtourMode, new MutableInt(1));

						}

					}

				}

				// PersonUtils.removeUnselectedPlans(person);
				// modifiedPopulationWriter.writePerson(person);
			}
		}

		System.out.println(shiftingScenario.mode2TripCounter);

		// modifiedPopulationWriter.closeStreaming();
	}

	public void assign() {
		String shift2mode = "preventedShoppingTrip";
		String shiftingType = shiftingScenario.type;
		int tripCounter = 0;
		double minTourDistance = 0.0;

		int leaveWhileLoopCounter = 0;
		// Part for subtourConversion
		if (shiftingType.equals("subtourConversion")) {

			shiftingScenario.toursToBeAssigned = (int) (shiftingScenario.subTourConversionRate
					* shiftingScenario.totalSubtourCounter.doubleValue());

			// Convert set to list in order to select random elements
			ArrayList<Id<Person>> agentCandidateList = new ArrayList<Id<Person>>(shiftingScenario.agentSet);

			int couterLimit = agentCandidateList.size() * 10;

			while (leaveWhileLoopCounter < couterLimit) {
				leaveWhileLoopCounter++;

				if (shiftingScenario.assignedSubTours >= shiftingScenario.toursToBeAssigned) {
					break;
				}

				int randomAgentCandidateIdx = (int) (Math.random() * (agentCandidateList.size() - 1));
				Id<Person> personId = agentCandidateList.get(randomAgentCandidateIdx);

				// Select a random person from agentSet
				int serviceCounter = 0;
				Plan plan = scenario.getPopulation().getPersons().get(personId).getSelectedPlan();

				// Loop over all subtours of this agent

				for (Subtour subTour : TripStructureUtils.getSubtours(plan, blackList)) {

					double estimatedTourDistance = getBeelineTourLength(subTour);
					// Get subtour mode
					String subtourMode = getSubtourMode(subTour, plan);

					// if (subtourMode.equals("walk")) {
					// minTourDistance = 2500.0;
					// } else if (subtourMode.equals("bike")) {
					// minTourDistance = 10000.0;
					// }

					// Check if this subtour can be shifted to an other mode
					// It is not allowed to shift an already shifted tour
					if (assignTourValidator.isValidSubTour(subTour) && (subtourMode != shift2mode)
							&& estimatedTourDistance > minTourDistance) {
						leaveWhileLoopCounter = 0;

						// System.out.println("Trip Size:" + subTour.getTrips().size());

						PlanElement searchPlanElement = (PlanElement) subTour.getTrips().get(0).getOriginActivity();
						int planElementToBeModifiedIdx = PopulationUtils.getActLegIndex(plan, searchPlanElement);

						Activity testAct = (Activity) plan.getPlanElements().get(planElementToBeModifiedIdx);

						// System.out.println(testAct);
						//if (!(serviceCounter > 0)) {
							testAct.getAttributes().putAttribute("jobId", "-99");
						//}
						serviceCounter++;

						for (Trip trip : subTour.getTrips()) {
							for (Leg l : trip.getLegsOnly()) {
								l.setRoute(null);
								l.setTravelTime(0.0);

								TripRouter.insertTrip(plan, trip.getOriginActivity(),
										Collections.singletonList(PopulationUtils.createLeg(shift2mode)),
										trip.getDestinationActivity());
							}

							if (shiftingScenario.mode2ShiftedTripCounter.containsKey(subtourMode)) {
								shiftingScenario.mode2ShiftedTripCounter.get(subtourMode).increment();

							} else {
								shiftingScenario.mode2ShiftedTripCounter.put(subtourMode, new MutableInt(1));

							}

							tripCounter++;
						}

						shiftingScenario.assignedSubTours++;

					}

				}
				// If no tour for this agent can be found, this agent could be deleted from
				// agentCandidateList
				// if (foundTour == false) {
				// agentCandidateList.remove(randomAgentCandidateIdx);
				//
				// }

			}
			System.out.println(shiftingScenario.assignedSubTours + " out of " + shiftingScenario.toursToBeAssigned);

		}

		// Write population
		// for (Person person : scenario.getPopulation().getPersons().values()) {
		// modifiedPopulationWriter.writePerson(person);
		// }

		new PopulationWriter(scenario.getPopulation(), null).write(modPlansFile);
		// modifiedPopulationWriter.startStreaming(modPlansFile);
		// modifiedPopulationWriter.closeStreaming();
		// System.out.println(tripCounter);

	}

	public String getSubtourMode(Subtour subTour, Plan plan) {

		// ToDo: Inefficient way to get the legMode. Reduce loops!
		String subtourMode = null;
		List<Trip> trips = subTour.getTrips();

		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImplFallback();

		for (TripStructureUtils.Trip trip : trips) {
			final List<PlanElement> fullTrip = plan.getPlanElements().subList(
					plan.getPlanElements().indexOf(trip.getOriginActivity()) + 1,
					plan.getPlanElements().indexOf(trip.getDestinationActivity()));
			subtourMode = mainModeIdentifier.identifyMainMode(fullTrip);
			return subtourMode;
		}

		return subtourMode;

	}

	public double getBeelineTourLength(Subtour subTour) {
		double distance = 0;
		for (Trip trip : subTour.getTrips()) {

			Coord fromCoord = trip.getOriginActivity().getCoord();
			Coord toCoord = trip.getDestinationActivity().getCoord();

			distance = distance + DistanceUtils.calculateDistance(fromCoord, toCoord);

		}
		return distance;
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
