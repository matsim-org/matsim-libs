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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.GarageParkingSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.IllegalParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomSearch;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.ParkingStrategyManager;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingLoader;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrixFromStoredTable;

public class MainPPSimZurich30km {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Todo change these three paths and try run.

		//String plansFile = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/singleAgentPlan_1000802.xml";
		 String plansFile =
		 "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/1pml_plans_30km.xml.gz";
		// String plansFile =
		//	 "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/1pct_plans_30km.xml.gz";
		 
		// String plansFile =
		// "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/10pct_plans_30km.xml.gz";
		String networkFile = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz";
		String facilititiesPath = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_facilities.xml.gz";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);

		addParkingActivityAndWalkLegToPlans(scenario.getPopulation().getPersons().values());

		String outputFolder = "C:/data/parkingSearch/psim/zurich/output/";

		Message.ttMatrix = new TTMatrixFromStoredTable("C:/data/parkingSearch/psim/zurich/inputs/it.50.3600secBin.ttMatrix.txt",
				scenario.getNetwork());

		// TODO: set strategies initially at random

		LinkedList<AgentWithParking> agentsMessage = new LinkedList<AgentWithParking>();

		LinkedList<ParkingSearchStrategy> allStrategies = new LinkedList<ParkingSearchStrategy>();
		allStrategies.add(new RandomSearch(300.0));
		allStrategies.add(new GarageParkingSearch());
		allStrategies.add(new IllegalParking());

		AgentWithParking.parkingStrategyManager = new ParkingStrategyManager(allStrategies);
		AgentWithParking.parkingManager = ParkingLoader.getParkingManagerZH(scenario.getNetwork(), Message.ttMatrix);

		AgentWithParking.parkingManager.initFirstParkingOfDay(scenario.getPopulation());

		// TODO: load parking infrastructure files from:
		// Z:\data\experiments\TRBAug2011\parkings

		int writeEachNthIteration = 5;
		int skipOutputInIteration = 0;

		for (int iter = 0; iter < 1; iter++) {

			EventsManager eventsManager = EventsUtils.createEventsManager();
			LegHistogram lh = new LegHistogram(300);
			EventWriterXML eventsWriter = new EventWriterXML(outputFolder + "events.xml.gz");
			if (writeOutput(writeEachNthIteration, skipOutputInIteration, iter)) {
				eventsManager.addHandler(eventsWriter);
				eventsManager.addHandler(lh);

				eventsManager.resetHandlers(0);
				eventsWriter.init(outputFolder + "events.xml.gz");

				eventsManager.resetHandlers(0);

				eventsWriter.init(outputFolder + "it." + iter + ".events.xml");
			}

			agentsMessage.clear();
			for (Person p : scenario.getPopulation().getPersons().values()) {
				agentsMessage.add(new AgentWithParking(p));
				AgentWithParking.parkingStrategyManager.prepareStrategiesForNewIteration(p, iter);
			}

			Mobsim sim = new ParkingPSim(scenario, eventsManager, agentsMessage);
			sim.run();
			eventsManager.finishProcessing();

			if (writeOutput(writeEachNthIteration, skipOutputInIteration, iter)) {
				lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_all.png");
				lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_car.png", TransportMode.car);
				lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_pt.png", TransportMode.pt);
				try {
					lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_ride.png", TransportMode.ride);
				} catch (Exception e) {

				}
				lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_walk.png", TransportMode.walk);
				eventsWriter.reset(0);
			}

			AgentWithParking.parkingStrategyManager.printStrategyStatistics();

		}

	}

	private static boolean writeOutput(int writeEachNthIteration, int skipOutputInIteration, int iter) {
		return iter % writeEachNthIteration == 0 && iter != skipOutputInIteration;
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

}
