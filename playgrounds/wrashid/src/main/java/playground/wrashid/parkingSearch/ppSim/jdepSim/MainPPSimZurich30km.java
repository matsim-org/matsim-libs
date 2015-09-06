/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.wrashid.parkingSearch.ppSim.jdepSim;

import org.apache.log4j.Logger;
import org.matsim.analysis.LegHistogram;
import org.matsim.analysis.LegHistogramChart;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.TTMatrixBasedTravelTime;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.TollAreaTravelDisutility;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingMemory;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.ParkingStrategyManager;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random.RandomNumbers;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.score.ParkingScoreEvaluator;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.*;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrixFromStoredTable;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MainPPSimZurich30km {

	private static final Logger log = Logger.getLogger(MainPPSimZurich30km.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ZHScenarioGlobal.config = ConfigUtils.loadConfig(args[0]);
		ZHScenarioGlobal.init();

		String plansFile = ZHScenarioGlobal.loadStringParam("plansFile");
		String networkFile = ZHScenarioGlobal.loadStringParam("networkFile");
		String facilititiesPath = ZHScenarioGlobal.loadStringParam("facilitiesFile");
		ZHScenarioGlobal.scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		Scenario scenario = ZHScenarioGlobal.scenario;
		ZHScenarioGlobal.initNetworkLinkSlopes();

		filterPopulationWithinScenarioRadius(scenario);
		removeNotSelectedPlans(scenario);
		multiplyPopulation(scenario, ZHScenarioGlobal.populationExpensionFactor);

		addParkingActivityAndWalkLegToPlans(scenario.getPopulation().getPersons().values());

		// this is introduced to quickly avoid problems with current
		// implemenation
		// regarding departure and arrival of car on same link
		// could be solve/updated in future.
		replaceCarLegsStartingAndEndingOnSameRoadByWalkLegs(scenario);

		ZHScenarioGlobal.initialRoutes = getInitialRoutes(scenario);

		Message.ttMatrix = new TTMatrixFromStoredTable(ZHScenarioGlobal.loadStringParam("ttMatrixFile"), scenario.getNetwork());

		LinkedList<ParkingSearchStrategy> allStrategies = ParkingStrategyScenarios.getScenarioStrategies(scenario);

		AgentWithParking.parkingStrategyManager = new ParkingStrategyManager(allStrategies);

		LinkedList<AgentWithParking> agentsMessage = new LinkedList<AgentWithParking>();

		initGlobalReroute(scenario);
		AgentWithParking.parkingManager = ParkingLoader.getParkingManagerZH(scenario.getNetwork(), Message.ttMatrix);
		
		if (ZHScenarioGlobal.loadBooleanParam("performCalibration")) {
			addPrivateParkingAtEachActivityLocationWithCapacityZero(AgentWithParking.parkingManager);
		}

		ParkingPersonalBetas parkingPersonalBetas = new ParkingPersonalBetas(scenario,
				HouseHoldIncomeZH.getHouseHoldIncomeCantonZH((ScenarioImpl) scenario));

		parkingPersonalBetas.externalWalkFactor = ZHScenarioGlobal.loadDoubleParam("ParkingPersonalBetas.externalWalkFactor");
		parkingPersonalBetas.externalSearchFactor = ZHScenarioGlobal.loadDoubleParam("ParkingPersonalBetas.externalSearchFactor");

		ParkingCostCalculatorZH parkingCostCalculatorZH = (ParkingCostCalculatorZH) AgentWithParking.parkingManager
				.getParkingCostCalculator();
		ZHScenarioGlobal.parkingScoreEvaluator = new ParkingScoreEvaluator(parkingCostCalculatorZH, parkingPersonalBetas);
		ZHScenarioGlobal.init(Message.ttMatrix, scenario.getNetwork());

		// TODO: we need to do that probably also inside loop below at start of
		// each iteration

		// TODO: load parking infrastructure files from:
		// Z:\data\experiments\TRBAug2011\parkings

		for (int iter = 0; iter < ZHScenarioGlobal.numberOfIterations; iter++) {
			log.info("iteration-" + iter + " starts");
			ZHScenarioGlobal.iteration = iter;

			if (ZHScenarioGlobal.loadBooleanParam("performCalibration")) {
				performAutoCalibration();
			}
			
			if (ZHScenarioGlobal.paramterExists("special.scenario.changeSupply") && ZHScenarioGlobal.loadBooleanParam("special.scenario.changeSupply")) {
				scenario_changeSupply();
				
			}

			EventsManager eventsManager = EventsUtils.createEventsManager();
			EventWriterXML eventsWriter = new EventWriterXML(ZHScenarioGlobal.outputFolder + "events.xml.gz");
			LegHistogram lh = new LegHistogram(300);
			if (ZHScenarioGlobal.writeOutputInCurrentIteration() && ZHScenarioGlobal.loadBooleanParam("doWriteEvents")) {
				eventsManager.addHandler(lh);
				eventsManager.addHandler(eventsWriter);

				eventsManager.resetHandlers(0);
				eventsWriter.init(ZHScenarioGlobal.getItersFolderPath() + "events.xml.gz");

				eventsManager.resetHandlers(0);

				eventsWriter.init(ZHScenarioGlobal.getItersFolderPath() + "it." + iter + ".events.xml.gz");

			}

			agentsMessage.clear();
			for (Person p : scenario.getPopulation().getPersons().values()) {
				agentsMessage.add(new AgentWithParking(p));
				AgentWithParking.parkingStrategyManager.prepareStrategiesForNewIteration(p, iter);
			}
			AgentWithParking.parkingManager.initFirstParkingOfDay(scenario.getPopulation());
			ZHScenarioGlobal.reset();
			AgentWithParking.reset();
			ParkingMemory.prepareForNextIteration();
			ParkingMemory.resetMemory();
			RandomNumbers.reset();

			log.info("simulation-" + iter + " starts");
			Mobsim sim = new ParkingPSim(scenario, eventsManager, agentsMessage);
			sim.run();
			eventsManager.finishProcessing();

			log.info("simulation-" + iter + " ended");

			if (ZHScenarioGlobal.writeOutputInCurrentIteration() && ZHScenarioGlobal.loadBooleanParam("doWriteEvents")) {
				LegHistogramChart.writeGraphic(lh, ZHScenarioGlobal.getItersFolderPath() + "it." + iter + ".legHistogram_all.png");
				LegHistogramChart.writeGraphic(lh, ZHScenarioGlobal.getItersFolderPath() + "it." + iter + ".legHistogram_car.png", TransportMode.car);
				LegHistogramChart.writeGraphic(lh, ZHScenarioGlobal.getItersFolderPath() + "it." + iter + ".legHistogram_pt.png", TransportMode.pt);
				try {
					LegHistogramChart.writeGraphic(lh, ZHScenarioGlobal.getItersFolderPath() + "it." + iter + ".legHistogram_ride.png",
                            TransportMode.ride);
				} catch (Exception e) {

				}
				LegHistogramChart.writeGraphic(lh, ZHScenarioGlobal.getItersFolderPath() + "it." + iter + ".legHistogram_walk.png", TransportMode.walk);

				eventsWriter.reset(0);
			}

			ZHScenarioGlobal.produceOutputStats();

			AgentWithParking.parkingStrategyManager.printStrategyStatistics();
			AgentWithParking.parkingStrategyManager.writeStatisticsToFile();
			AgentWithParking.parkingStrategyManager.reset();
			AgentWithParking.parkingManager.reset();
			resetRoutes(scenario);
			setParkingLinkIdToClosestActivity(scenario.getPopulation().getPersons().values());
			log.info("number of numberOfTollAreaEntryEvents: " + AgentWithParking.numberOfTollAreaEntryEvents);

			log.info("iteration-" + iter + " ended");
		}

		System.exit(0);

	}

	private static void addPrivateParkingAtEachActivityLocationWithCapacityZero(ParkingManagerZH parkingManager) {
		// add only, if not only exists such a parking

		int initialNumberOfParkings=parkingManager.getParkings().size();
		// activity facility Id, activityType, parking facility id
		TwoHashMapsConcatenated<Id, String, Id> privateParkingFacilityIdMapping = new TwoHashMapsConcatenated<Id, String, Id>();

		for (PParking parking : parkingManager.getParkings()) {
			if (parking.getId().toString().contains("private")) {
				PrivateParking privateParking = (PrivateParking) parking;

				privateParkingFacilityIdMapping.put(privateParking.getActInfo().getFacilityId(), privateParking.getActInfo()
						.getActType(), parking.getId());
			}
		}

		int j = 0;
		for (Person p : ZHScenarioGlobal.scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();

			int i = 0;

			while (i < planElements.size()) {
				if (planElements.get(i) instanceof Activity) {
					Activity act = (Activity) planElements.get(i);

					if (privateParkingFacilityIdMapping.get(act.getFacilityId(), act.getType()) == null) {
						ActInfo actInfo = new ActInfo(act.getFacilityId(), act.getType());
						PrivateParking privateParking = new PrivateParking(act.getCoord(), actInfo);
						privateParking.setParkingId(Id.create("privateParkings-x-" + j, PParking.class));
						privateParking.setType("private");
						j++;

						privateParkingFacilityIdMapping.put(act.getFacilityId(), act.getType(), privateParking.getId());
						parkingManager.getParkings().add(privateParking);
					}
				}
				i++;
			}
		}

		parkingManager.reset();

		System.out.println("numberOfPrivateParking facilities added (with capacity zero) :" + parkingManager.getParkings().size());
		System.out.println();
	}
	
	
	private static void scenario_changeSupply(){
		if (ZHScenarioGlobal.iteration==ZHScenarioGlobal.loadIntParam("special.scenario.changeSupply.atIteration")){
			double scaling = ZHScenarioGlobal.loadDoubleParam("special.scenario.changeSupply.newScaling");
			LinkedList<PParking> parkings = AgentWithParking.parkingManager.getParkings();
			for (PParking parking : parkings) {
				int newCapacity = (int) Math.round(parking.getIntCapacity()*scaling);
				parking.setCapacity(newCapacity);
			}
			AgentWithParking.parkingManager.resetFirstAdaptedLegCache();
		}
	}

	private static void performAutoCalibration() {
		LinkedList<PParking> parkings = AgentWithParking.parkingManager.getParkings();
		
		if (ZHScenarioGlobal.privateParkingCalibrationOriginalCapacities==null){
			ZHScenarioGlobal.privateParkingCalibrationOriginalCapacities=new IntegerValueHashMap<Id>();
			
			for (PParking parking : parkings) {
				if (parking.getId().toString().contains("private")) {
					ZHScenarioGlobal.privateParkingCalibrationOriginalCapacities.set(parking.getId(), parking.getIntCapacity());
				}
			}
		}
		
		
		double newScalingFactor= ZHScenarioGlobal.loadDoubleParam("performCalibration.startValue") + ZHScenarioGlobal.iteration* ZHScenarioGlobal.loadDoubleParam("performCalibration.stepValue");
		log.info("newScalingFactor: " + newScalingFactor);
		
		for (PParking parking : parkings) {
			if (parking.getId().toString().contains("private")) {
				int newCapacity = (int) Math.round(ZHScenarioGlobal.privateParkingCalibrationOriginalCapacities.get(parking.getId())*newScalingFactor);
				parking.setCapacity(newCapacity);
			}
		}
		
		//Random rand = RandomNumbers.getGlobalbRandom();
		// double
		// countsScalingFactor=ZHScenarioGlobal.loadDoubleParam("ComparisonGarageCounts.countsScalingFactor")*rand.nextDouble()*0.2;

		// ZHScenarioGlobal.config.setParam("parkingSearch",
		// "ComparisonGarageCounts.countsScalingFactor",
		// Double.toString(countsScalingFactor));

		// log.info("countsScalingFactor=" + countsScalingFactor);

		// TODO: print private parking assignment later here

		
		
		
		
		
		
		
		/*
		
		double distance = 100;
		LinkedList<Parking> parkings = AgentWithParking.parkingManager.getParkings();

		EnclosingRectangle rect = new EnclosingRectangle();

		for (Parking parking : parkings) {
			rect.registerCoord(parking.getCoord());
		}
		QuadTree<Parking> privateParkingsQuadTree = (new QuadTreeInitializer<Parking>()).getQuadTree(rect);
		QuadTree<Parking> garageParkingsQuadTree = (new QuadTreeInitializer<Parking>()).getQuadTree(rect);

		for (Parking parking : parkings) {
			if (parking.getId().toString().contains("private")) {
				privateParkingsQuadTree.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
			}
			
			if (parking.getId().toString().contains("gp")) {
				garageParkingsQuadTree.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
			}
		}

		for (Parking parking : parkings) {
			if (parking.getId().toString().contains("private")) {
				Collection<Parking> collection = privateParkingsQuadTree.get(parking.getCoord().getX(), parking.getCoord().getY(),
						distance);

//				LinkedList<Parking> remove = new LinkedList<Parking>();
//				for (Parking p : collection) {
//					if (p.getIntCapacity() == 0) {
//						collection.removeAll(remove);
//					}
//				}
				
				Parking closestGP = garageParkingsQuadTree.get(parking.getCoord().getX(), parking.getCoord().getY());
				
				if (GeneralLib.getDistance(closestGP.getCoord(), parking.getCoord())<300){
					if (ComparisonGarageCounts.relativeErrorCounts.containsKey(closestGP.getId()) && ComparisonGarageCounts.relativeErrorCounts.get(closestGP.getId())<0.5){
						continue;
					}
				}
				
				
				if (parking.getIntCapacity()==0){
					continue;
				}

				if (collection.size() > 0) {
					int i = rand.nextInt(collection.size());

					for (Parking p : collection) {
						i--;

						if (i == 0) {
							int cap = parking.getIntCapacity();
							int keep = rand.nextInt(cap);
							int giveAway = cap - keep;
							parking.setCapacity(keep);
							p.setCapacity(p.getIntCapacity() + giveAway);
							break;
						}
					}
				}
			}
		}
		*/
	}

	public static void initGlobalReroute(Scenario scenario) {
		if (ZHScenarioGlobal.loadDoubleParam("radiusTolledArea") > 0) {

			TravelDisutility travelCost = new TollAreaTravelDisutility();
			TravelTime travelTime = new TTMatrixBasedTravelTime(Message.ttMatrix);
			Dijkstra routingAlgo = new Dijkstra(scenario.getNetwork(), travelCost, travelTime);

			EditRoute.globalEditRoute = new EditRoute(Message.ttMatrix, scenario.getNetwork(), routingAlgo);
		} else {
			EditRoute.globalEditRoute = new EditRoute(Message.ttMatrix, scenario.getNetwork());
		}
	}

	private static void resetRoutes(Scenario scenario) {
		TwoHashMapsConcatenated<Id, Integer, LinkNetworkRouteImpl> initialRoutes = ZHScenarioGlobal.initialRoutes;

		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();

			int i = 0;
			while (i < planElements.size()) {
				if (planElements.get(i) instanceof LegImpl) {
					Leg leg = (Leg) planElements.get(i);

					if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
						leg.setRoute(initialRoutes.get(p.getId(), i).clone());
					}

				}
				i++;
			}
		}
	}

	// personId, legIndex, route
	private static TwoHashMapsConcatenated<Id, Integer, LinkNetworkRouteImpl> getInitialRoutes(Scenario scenario) {
		TwoHashMapsConcatenated<Id, Integer, LinkNetworkRouteImpl> result = new TwoHashMapsConcatenated<Id, Integer, LinkNetworkRouteImpl>();

		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();

			int i = 0;
			while (i < planElements.size()) {
				if (planElements.get(i) instanceof LegImpl) {
					Leg leg = (Leg) planElements.get(i);

					if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
						LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
						result.put(p.getId(), i, route.clone());
					}

				}
				i++;
			}
		}

		return result;
	}

	private static void multiplyPopulation(Scenario scenario, int populationExpansionFactor) {
		LinkedList<Person> originalAgents = new LinkedList<Person>();

		for (Person p : scenario.getPopulation().getPersons().values()) {
			originalAgents.add(p);
		}

		for (Person p : originalAgents) {
			scenario.getPopulation().getPersons().remove(p.getId());
		}

		PopulationFactory factory = scenario.getPopulation().getFactory();

		int pCounter = 1;

		for (int i = 0; i < populationExpansionFactor; i++) {
			for (Person origPerson : originalAgents) {
				Person originPerson = origPerson;

				Person newPerson = factory.createPerson(Id.create(String.valueOf(pCounter++), Person.class));
				PersonImpl.setAge(newPerson, PersonImpl.getAge(origPerson));
				PersonImpl.setSex(newPerson, PersonImpl.getSex(origPerson));
				newPerson.addPlan(originPerson.createCopyOfSelectedPlanAndMakeSelected());

				scenario.getPopulation().addPerson(newPerson);
			}
		}

		System.out.println("population after population expansion: " + scenario.getPopulation().getPersons().size());
	}

	private static void removeNotSelectedPlans(Scenario scenario) {
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();
			LinkedList<Plan> selectedPlanList = new LinkedList<Plan>();
			selectedPlanList.add(selectedPlan);
			p.getPlans().retainAll(selectedPlanList);
		}
	}

	private static void filterPopulationWithinScenarioRadius(Scenario scenario) {
		Coord coordinatesLindenhofZH = ParkingHerbieControler.getCoordinatesLindenhofZH();
		LinkedList<Id> personToBeRemoved = new LinkedList<Id>();

		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();

			int i = 0;
			boolean removeFromPopulatation = true;
			while (i < planElements.size()) {
				if (planElements.get(i) instanceof Activity) {
					Activity act = (Activity) planElements.get(i);

					if (GeneralLib.getDistance(coordinatesLindenhofZH, act.getCoord()) < ZHScenarioGlobal
							.loadDoubleParam("scenarioRadiusInMeters")) {
						removeFromPopulatation = false;
						break;
					}
				}
				i++;
			}

			if (removeFromPopulatation) {
				personToBeRemoved.add(p.getId());
			}
		}

		for (Id personId : personToBeRemoved) {
			scenario.getPopulation().getPersons().remove(personId);
		}

		System.out.println("population size after fitering: " + scenario.getPopulation().getPersons().size());
	}

	private static void replaceCarLegsStartingAndEndingOnSameRoadByWalkLegs(Scenario scenario) {
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();

			int i = 0;
			while (i < planElements.size()) {
				if (planElements.get(i) instanceof LegImpl) {
					Activity prevAct = (Activity) planElements.get(i - 1);
					Leg leg = (Leg) planElements.get(i);
					Activity nextAct = (Activity) planElements.get(i + 1);

					if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {

						if (prevAct.getLinkId().toString().equalsIgnoreCase(nextAct.getLinkId().toString())) {
							double walkDistance = GeneralLib.getDistance(prevAct.getCoord(), nextAct.getCoord());
							// if (walkDistance<300){
							// TODO: improve this later (no straight line)
							double walkSpeed = 3.0 / 3.6;
							double walkDuration = walkDistance / walkSpeed;

							LegImpl walkLeg = new LegImpl(TransportMode.walk);
							walkLeg.setTravelTime(walkDuration);
							planElements.remove(i);
							planElements.add(i, walkLeg);
							// }
						}
					}
				}

				i++;
			}
		}
	}

	private static void addParkingActivityAndWalkLegToPlans(Collection<? extends Person> persons) {
		for (Person p : persons) {
			Plan selectedPlan = p.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();

			int i = 0;
			while (i < planElements.size()) {
				if (planElements.get(i) instanceof LegImpl) {
					Activity prevAct = (Activity) planElements.get(i - 1);
					Leg leg = (Leg) planElements.get(i);
					Activity nextAct = (Activity) planElements.get(i + 1);

					if (leg.getMode().equalsIgnoreCase(TransportMode.car) && !nextAct.getType().equalsIgnoreCase("parking")) {

						ActivityImpl parkingAct = new ActivityImpl("parking", nextAct.getCoord(), nextAct.getLinkId());
						parkingAct.setEndTime(nextAct.getStartTime()); // replace
																		// this
																		// during
																		// parking!
						planElements.add(i + 1, parkingAct);

						LegImpl walkLeg = new LegImpl(TransportMode.walk);
						// just initializing for testing (should be overwritten
						// at end of parking search)
						walkLeg.setTravelTime(3600);
						planElements.add(i + 2, walkLeg);
					}

					if (leg.getMode().equalsIgnoreCase(TransportMode.car) && !prevAct.getType().equalsIgnoreCase("parking")) {
						ActivityImpl parkingAct = new ActivityImpl("parking", prevAct.getCoord(), prevAct.getLinkId());
						parkingAct.setEndTime(prevAct.getEndTime()); // replace
																		// this
																		// during
																		// parking!
						planElements.add(i, parkingAct);

						LegImpl walkLeg = new LegImpl(TransportMode.walk);
						// just initializing for testing (should be overwritten
						// at end of parking search)
						walkLeg.setTravelTime(3600);
						planElements.add(i, walkLeg);
					}
				}
				i++;
			}

			DebugLib.emptyFunctionForSettingBreakPoint();
		}
	}

	private static void setParkingLinkIdToClosestActivity(Collection<? extends Person> persons) {
		for (Person p : persons) {
			Plan selectedPlan = p.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();

			int i = 0;
			while (i < planElements.size()) {
				if (planElements.get(i) instanceof LegImpl) {
					ActivityImpl prevAct = (ActivityImpl) planElements.get(i - 1);
					Leg leg = (Leg) planElements.get(i);
					ActivityImpl nextAct = (ActivityImpl) planElements.get(i + 1);

					if (leg.getMode().equalsIgnoreCase(TransportMode.walk) && prevAct.getType().equalsIgnoreCase("parking")) {
						prevAct.setLinkId(nextAct.getLinkId());
					}

					if (leg.getMode().equalsIgnoreCase(TransportMode.walk) && nextAct.getType().equalsIgnoreCase("parking")) {
						nextAct.setLinkId(prevAct.getLinkId());
					}
				}
				i++;
			}
		}
	}

}
