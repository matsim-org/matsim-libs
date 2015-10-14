/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsInEvacuationAreaCounter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Counts the number of agents within an evacuated area.
 * The count is split up into transport modes and activity performing.
 * 
 * @author cdobler
 */
public class AgentsInEvacuationAreaCounter implements LinkEnterEventHandler,
		PersonArrivalEventHandler, PersonDepartureEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		PersonStuckEventHandler, IterationEndsListener,
		MobsimInitializedListener {

	private static final Logger log = Logger.getLogger(AgentsInEvacuationAreaCounter.class);

//	protected int binSize = 300; // by default 5 minutes
	protected int binSize = 60; // by default 1 minute
	protected int nofPictureBins = 36 * 3600 / binSize; // by default 36 hours
	protected int nofBins = 96 * 3600 / binSize; // by default 96 hours
	protected int currentBin = 0; // current time bin slot

	protected final Scenario scenario;
	protected final Set<String> transportModes;
	protected final CoordAnalyzer coordAnalyzer;
	protected final DecisionDataProvider decisionDataProvider;
	protected final double scaleFactor;
	
	protected Set<Id> activityAgentsInEvacuationArea;
	protected Map<String, Set<Id>> legAgentsInEvacuationArea;
	protected Map<Id, String> currentTransportMode;
	
	protected Set<Id> vehiclesWithoutDriver;
	protected Map<Id, Id> driverVehicleMap;
	protected Map<Id, List<Id>> vehiclePassengersMap;	// the driver is not included!
	
	protected int[] activityBins;
	protected int[] activityBinsParticipating;
	protected int[] activityBinsNotParticipating;
	protected Map<String, int[]> legBins;
	protected Map<String, int[]> legBinsParticipating;
	protected Map<String, int[]> legBinsNotParticipating;
	protected Map<Id, Tuple<String, Double>> leftByMode;
	
	protected Map<Integer, Set<Id>> undefinedActivityAgentBins;
	protected Map<Integer, Map<String, Set<Id>>> undefinedLegAgentBins;
	
	public AgentsInEvacuationAreaCounter(Scenario scenario, Set<String> transportModes, CoordAnalyzer coordAnalyzer,
			DecisionDataProvider decisionDataProvider, double scaleFactor) {
		this.scenario = scenario;
		this.transportModes = transportModes;
		this.coordAnalyzer = coordAnalyzer;
		this.scaleFactor = scaleFactor;
		this.decisionDataProvider = decisionDataProvider;
		
		init();
	}

	private void init() {
		currentTransportMode = new HashMap<Id, String>();
		activityAgentsInEvacuationArea = new HashSet<Id>();
		legAgentsInEvacuationArea = new HashMap<String, Set<Id>>();
		vehiclesWithoutDriver = new HashSet<Id>();
		driverVehicleMap = new HashMap<Id, Id>();
		vehiclePassengersMap = new HashMap<Id, List<Id>>();
		leftByMode = new HashMap<Id, Tuple<String, Double>>();
		activityBins = new int[nofBins];
		activityBinsParticipating = new int[nofBins];
		activityBinsNotParticipating = new int[nofBins];
		legBins = new TreeMap<String, int[]>();
		legBinsParticipating = new TreeMap<String, int[]>();
		legBinsNotParticipating = new TreeMap<String, int[]>();
		undefinedActivityAgentBins = new HashMap<Integer, Set<Id>>();
		undefinedLegAgentBins = new HashMap<Integer, Map<String, Set<Id>>>();
		
		for (String string : transportModes) {
			legAgentsInEvacuationArea.put(string, new HashSet<Id>());
			legBins.put(string, new int[nofBins]);
			legBinsParticipating.put(string, new int[nofBins]);
			legBinsNotParticipating.put(string, new int[nofBins]);
		}
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (Person person : scenario.getPopulation().getPersons().values()) {

			if (person.getSelectedPlan().getPlanElements().size() == 0) continue;
			else {
				Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);

				boolean isAffected = false;
				if (activity.getFacilityId() != null) {
					ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId());
					isAffected = this.coordAnalyzer.isFacilityAffected(facility);
				} else {
					Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());
					isAffected = this.coordAnalyzer.isLinkAffected(link);
					log.warn("No facility defined in activity - taking coordinate from activity...");
				}

				if (isAffected) activityAgentsInEvacuationArea.add(person.getId());
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		updateBinData(event.getTime());

		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());

		String transportMode = currentTransportMode.get(event.getDriverId());
		Set<Id> set = legAgentsInEvacuationArea.get(transportMode);

		Id vehicleId = driverVehicleMap.get(event.getDriverId());
		List<Id> passengers = null;
		Set<Id> passengerSet = null;
		if (vehicleId != null) {
			passengers = vehiclePassengersMap.get(vehicleId);
			passengerSet = legAgentsInEvacuationArea.get(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE);
		}
		
		boolean wasAffected = set.contains(event.getDriverId());
		
		if (this.coordAnalyzer.isLinkAffected(link)) {
			set.add(event.getDriverId());
			if (passengers != null) {
				for (Id passengerId : passengers) passengerSet.add(passengerId);
			}
			
			// if the agent just entered the evacuation area, reset its left mode
			if (!wasAffected && event.getTime() >= EvacuationConfig.evacuationTime) {
				this.leftByMode.remove(event.getDriverId());
				if (passengers != null) {
					for (Id passengerId : passengers) this.leftByMode.remove(passengerId);
				}
			}
		} else {
			set.remove(event.getDriverId());
			if (passengers != null) {
				for (Id passengerId : passengers) passengerSet.remove(passengerId);
			}
			
			// if the agents just left the evacuation area set its left mode
			if (wasAffected && event.getTime() >= EvacuationConfig.evacuationTime) {
				this.leftByMode.put(event.getDriverId(), new Tuple<String, Double>(transportMode, event.getTime()));
				if (passengers != null) {
					for (Id passengerId : passengers) {
						this.leftByMode.put(passengerId, new Tuple<String, Double>(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE, event.getTime()));
					}
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		updateBinData(event.getTime());

		String transportMode = currentTransportMode.get(event.getPersonId());
		Set<Id> set = legAgentsInEvacuationArea.get(transportMode);
		set.remove(event.getPersonId());

		currentTransportMode.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		updateBinData(event.getTime());

		boolean isAffected = false;
		if (event.getFacilityId() != null) {
			ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(event.getFacilityId());
			isAffected = this.coordAnalyzer.isFacilityAffected(facility);
		} else {
			Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
			isAffected = this.coordAnalyzer.isLinkAffected(link);
			log.warn("No facilityId given - using link coordinates!");
		}

		if (isAffected) {
			activityAgentsInEvacuationArea.add(event.getPersonId());
		} else {
			activityAgentsInEvacuationArea.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		updateBinData(event.getTime());

		activityAgentsInEvacuationArea.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		updateBinData(event.getTime());

		String transportMode = event.getLegMode();
		currentTransportMode.put(event.getPersonId(), transportMode);

		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
		Set<Id> set = legAgentsInEvacuationArea.get(transportMode);
		if (this.coordAnalyzer.isLinkAffected(link)) {
			set.add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		updateBinData(event.getTime());

		String transportMode = event.getLegMode();
		/*
		 * When called from the cleanup method of the QSim, the transportMode is
		 * not set.
		 */
		if (transportMode != null) {
			Set<Id> set = legAgentsInEvacuationArea.get(transportMode);
			set.remove(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		/*
		 * We create a list where the passengers will be added.
		 * The driver is NOT added to the list!
		 */
		List<Id> passengers = vehiclePassengersMap.get(event.getVehicleId());
		if (passengers == null) {
			passengers = new ArrayList<Id>();
			vehiclePassengersMap.put(event.getVehicleId(), passengers);
		}
		
		boolean isDriver = currentTransportMode.get(event.getPersonId()).equals(TransportMode.car);
		if (isDriver) {
			driverVehicleMap.put(event.getPersonId(), event.getVehicleId());
		} else {			
			passengers.add(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		List<Id> passengers = vehiclePassengersMap.get(event.getVehicleId());
		
		// try to remove the person as driver
		Id driverId = driverVehicleMap.remove(event.getPersonId());
		
		// check whether the agent is the driver of the vehicle
		boolean isDriver;
		if (driverId == null) isDriver = false;
		else isDriver = true;
		
		if (isDriver) {	
			// if the vehicle is empty
			if (passengers.size() == 0) {
				vehiclePassengersMap.remove(event.getVehicleId());
				driverVehicleMap.remove(driverId);
			}
			// the driver leaves the car but some passengers are left
			else {
				vehiclesWithoutDriver.add(driverId);
				driverVehicleMap.remove(driverId);
			}
		}
		// it is a passenger
		else {			
			// remove it from the list
			passengers.remove(event.getPersonId());
			
			// if it is the last passenger and the driver has also already left the vehicle
			if (passengers.size() == 0 && vehiclesWithoutDriver.contains(event.getVehicleId())) {
				vehiclesWithoutDriver.remove(event.getVehicleId());
				vehiclePassengersMap.remove(event.getVehicleId());
			}
		}
	}

	@Override
	public void reset(int iteration) {
		currentBin = 0;
		activityAgentsInEvacuationArea.clear();
		vehiclesWithoutDriver.clear();
		driverVehicleMap.clear();
		vehiclePassengersMap.clear();
		activityBins = new int[nofBins];
		undefinedActivityAgentBins.clear();
		undefinedLegAgentBins.clear();
		
		for (String string : transportModes) {
			legAgentsInEvacuationArea.put(string, new HashSet<Id>());
			legBins.put(string, new int[nofBins]);
		}
		
		for (Set<Id> set : legAgentsInEvacuationArea.values()) {
			set.clear();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		// debug
		for (Id id : activityAgentsInEvacuationArea) {
			Participating participating = this.decisionDataProvider.getPersonDecisionData(id).getParticipating();
			if (participating == Participating.TRUE) {
				log.warn("Person " + id.toString() + " is still inside the evacuation area but should have left.");				
			}
		}

		// ensure, that the last bin is written
		updateBinData(currentBin + 1);

		// now process data that has been collected before agents have decided whether to evacuate or not
		postProcessUndefinedAgents();
		
		String fileName = null;

		AgentsInEvacuationAreaWriter writer = new AgentsInEvacuationAreaWriter(this.binSize, this.nofPictureBins, event.getIteration());

		/*
		 * write text files
		 */
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea.txt");
		writer.write(fileName, activityBins, legBins);

		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationAreaParticipating.txt");
		writer.write(fileName, activityBinsParticipating, legBinsParticipating);
		
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationAreaNotParticipating.txt");
		writer.write(fileName, activityBinsNotParticipating, legBinsNotParticipating);
		
		/*
		 * write activity performing graphs
		 */
		String title = "agents in evacuated area";
		String legend = "agents in evacuated area";
		
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_activity.png");
		writer.writeGraphic(fileName, title, legend, "activity", activityBins);

		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_activity_participating.png");
		writer.writeGraphic(fileName, title, legend, "activity", activityBinsParticipating);
		
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_activity_not_participating.png");
		writer.writeGraphic(fileName, title, legend, "activity", activityBinsNotParticipating);
		
		/*
		 * write leg performing graphs
		 */
		for (String legMode : transportModes) {
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_" + legMode + ".png");
			writer.writeGraphic(fileName, title, legend, legMode, legBins.get(legMode));
			
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_" + legMode + "_participating.png");
			writer.writeGraphic(fileName, title, legend, legMode, legBinsParticipating.get(legMode));
			
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_" + legMode + "_not_participating.png");
			writer.writeGraphic(fileName, title, legend, legMode, legBinsNotParticipating.get(legMode));
		}

		/*
		 * write all agents into a graph
		 */
		int[] allAgents = activityBins.clone();
		for (String legMode : transportModes) {
			int[] leg = legBins.get(legMode);
			for (int i = 0; i < allAgents.length; i++) {
				allAgents[i] = allAgents[i] + leg[i];
			}
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_allAgents.png");
			writer.writeGraphic(fileName, title, legend, "all agents", allAgents);
		}
		
		int[] allParticipatingAgents = activityBinsParticipating.clone();
		for (String legMode : transportModes) {
			int[] leg = legBinsParticipating.get(legMode);
			for (int i = 0; i < allParticipatingAgents.length; i++) {
				allParticipatingAgents[i] = allParticipatingAgents[i] + leg[i];
			}
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_allParticipatingAgents.png");
			writer.writeGraphic(fileName, title, legend, "participating agents", allParticipatingAgents);
		}
		
		int[] allNotParticipatingAgents = activityBinsNotParticipating.clone();
		for (String legMode : transportModes) {
			int[] leg = legBinsNotParticipating.get(legMode);
			for (int i = 0; i < allNotParticipatingAgents.length; i++) {
				allNotParticipatingAgents[i] = allNotParticipatingAgents[i] + leg[i];
			}
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_allNotParticipatingAgents.png");
			writer.writeGraphic(fileName, title, legend, "not participating agents", allNotParticipatingAgents);
		}
		
		/*
		 * write single graph with all agents
		 */
		String[] names = new String[transportModes.size() + 2];
		int[][] data = new int[transportModes.size() + 2][];
		names[0] = "all";
		data[0] = allAgents;
		names[1] = "activity";
		data[1] = activityBins;
		int i = 2;
		for (String legMode : transportModes) {
			data[i] = legBins.get(legMode);
			names[i] = legMode;
			i++;
		}
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_comparison.png");
		writer.writeGraphic(fileName, names, data);

		names[0] = "participating";
		data[0] = allParticipatingAgents;
		names[1] = "activity";
		data[1] = activityBinsParticipating;
		i = 2;
		for (String legMode : transportModes) {
			data[i] = legBinsParticipating.get(legMode);
			names[i] = legMode;
			i++;
		}
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_comparison_participating.png");
		writer.writeGraphic(fileName, names, data);
		
		names[0] = "not participating";
		data[0] = allNotParticipatingAgents;
		names[1] = "activity";
		data[1] = activityBinsNotParticipating;
		i = 2;
		for (String legMode : transportModes) {
			data[i] = legBinsNotParticipating.get(legMode);
			names[i] = legMode;
			i++;
		}
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_comparison_not_participating.png");
		writer.writeGraphic(fileName, names, data);
		
		/*
		 * Count number of evacuees per mode
		 */
		Map<String, int[]> leftByModeBins = new HashMap<String, int[]>();
		Map<String, int[]> participatingLeftByModeBins = new HashMap<String, int[]>();
		Map<String, int[]> notParticipatingLeftByModeBins = new HashMap<String, int[]>();
		Map<String, Integer> participatingEvacueesPerMode = new TreeMap<String, Integer>();
		Map<String, Integer> notParticipatingEvacueesPerMode = new TreeMap<String, Integer>();
		for (String transportMode : this.transportModes) {
			participatingEvacueesPerMode.put(transportMode, 0);
			notParticipatingEvacueesPerMode.put(transportMode, 0);
			leftByModeBins.put(transportMode, new int[nofBins]);
			participatingLeftByModeBins.put(transportMode, new int[nofBins]);
			notParticipatingLeftByModeBins.put(transportMode, new int[nofBins]);
		}
		for (Entry<Id, Tuple<String, Double>> entry : this.leftByMode.entrySet()) {
			Participating participating = this.decisionDataProvider.getPersonDecisionData(entry.getKey()).getParticipating();
			Tuple<String, Double> tuple = entry.getValue();
			String transportMode = tuple.getFirst();
			double time = tuple.getSecond();
			
			if (participating == Participating.TRUE) {
				int count = participatingEvacueesPerMode.get(transportMode);
				participatingEvacueesPerMode.put(transportMode, count + 1);
				
				int binIndex = getBinIndex(time);
				participatingLeftByModeBins.get(transportMode)[binIndex] = participatingLeftByModeBins.get(transportMode)[binIndex] + 1;
			} else if (participating == Participating.FALSE) {
				int count = notParticipatingEvacueesPerMode.get(transportMode);
				notParticipatingEvacueesPerMode.put(transportMode, count + 1);
				
				int binIndex = getBinIndex(time);
				notParticipatingLeftByModeBins.get(transportMode)[binIndex] = notParticipatingLeftByModeBins.get(transportMode)[binIndex] + 1;
			} 
			else throw new RuntimeException("Cannot handle agent since its participation state is undefined.");
		}
		
		/*
		 * So far each bin in (not)participatingLeftByModeBins contains the number of agents
		 * left in the current bin. We cummulate the values.
		 */		
		for (String transportMode : this.transportModes) {
			int participating = 0;
			int notParticipating = 0;
			int[] array = leftByModeBins.get(transportMode);
			int[] participatingArray = participatingLeftByModeBins.get(transportMode);
			int[] notParticipatingArray = notParticipatingLeftByModeBins.get(transportMode);
			for (int idx = 0; idx < this.nofBins; idx++) {
				participating += participatingArray[idx];
				notParticipating += notParticipatingArray[idx];
				participatingArray[idx] = (int) (participating * scaleFactor);
				notParticipatingArray[idx] = (int) (notParticipating  * scaleFactor);
				array[idx] = (int) ((participating + notParticipating) * scaleFactor);
			}
		}

		for (String transportMode : this.transportModes) {
			log.info("Participating agents using " + transportMode + " mode:\t" + participatingEvacueesPerMode.get(transportMode) * scaleFactor);
			log.info("Not participating agents using " + transportMode + " mode:\t" + notParticipatingEvacueesPerMode.get(transportMode) * scaleFactor);		
		}
		
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByModeTotal.txt");
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
			bw.write("transport mode");
			bw.write("\t");
			bw.write("participating");
			bw.write("\t");
			bw.write("not participating");
			bw.write("\t");
			bw.write("total");
			bw.write("\n");
			
			for (String transportMode : this.transportModes) {
				int participating = participatingEvacueesPerMode.get(transportMode);
				int notParticipating = notParticipatingEvacueesPerMode.get(transportMode);
				bw.write(transportMode);
				bw.write("\t");
				bw.write(String.valueOf(participating * scaleFactor));
				bw.write("\t");
				bw.write(String.valueOf(notParticipating * scaleFactor));
				bw.write("\t");
				bw.write(String.valueOf((participating + notParticipating) * scaleFactor));
				bw.write("\n");
			}
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByModeDetailed.txt");
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
			bw.write("agentId");
			bw.write("\t");
			bw.write("transport mode");
			bw.write("\t");
			bw.write("left evacuation area time");
			bw.write("\n");
			
			
			for (Entry<Id, Tuple<String, Double>> entry : this.leftByMode.entrySet()) {
				
				Id agentId = entry.getKey();
				String tansportMode = entry.getValue().getFirst();
				Double time = entry.getValue().getSecond();
				
				bw.write(agentId.toString());
				bw.write("\t");
				bw.write(tansportMode);
				bw.write("\t");
				bw.write(time.toString());
				bw.write("\n");
			}
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		/*
		 * Write data and use dummies for activities since we are only interested in people traveling.
		 */
		String absoluteFileName;
		String relativeFileName;
		absoluteFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_absolute.txt");
		relativeFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_relative.txt");
		writer.write(absoluteFileName, relativeFileName, leftByModeBins);
		
		absoluteFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_absolute_participating.txt");
		relativeFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_relative_participating.txt");
		writer.write(absoluteFileName, relativeFileName, participatingLeftByModeBins);
		
		absoluteFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_absolute_not_participating.txt");
		relativeFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_relative_not_participating.txt");
		writer.write(absoluteFileName, relativeFileName, notParticipatingLeftByModeBins);
		
		/*
		 * write leg performing graphs
		 */
		title = "agents left evacuated area";
		legend = "agents left evacuated area";
		for (String legMode : transportModes) {
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_" + legMode + ".png");
			writer.writeGraphic(fileName, title, legend, legMode, leftByModeBins.get(legMode));
			
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_" + legMode + "_participating.png");
			writer.writeGraphic(fileName, title, legend, legMode, participatingLeftByModeBins.get(legMode));
			
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsLeftEvacuationAreaByMode_" + legMode + "_not_participating.png");
			writer.writeGraphic(fileName, title, legend, legMode, notParticipatingLeftByModeBins.get(legMode));
		}
	}

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.nofBins - 1) {
			return this.nofBins - 1;
		}
		return bin;
	}

	/*
	 * If the given time is in a later time bin, we can calculate the values for
	 * the past time bins.
	 */
	private void updateBinData(double time) {
		int binIndex = getBinIndex(time);
		if (binIndex > currentBin) {

			int participatingActivityAgents = 0;
			int notParticipatingActivityAgents = 0;
			Set<Id> undefinedActivityAgents = new HashSet<Id>();
			for (Id activityAgentId : activityAgentsInEvacuationArea) {
				Participating participating = this.decisionDataProvider.getPersonDecisionData(activityAgentId).getParticipating();
				if (participating == Participating.TRUE) participatingActivityAgents++;
				else if (participating == Participating.FALSE) notParticipatingActivityAgents++;
				else if (participating == Participating.UNDEFINED) undefinedActivityAgents.add(activityAgentId);
				else throw new RuntimeException("Undefined participation state found: " + participating.toString());
			}
			
			Map<String, Integer> participatingLegAgents = new HashMap<String, Integer>();
			Map<String, Integer> notParticipatingLegAgents = new HashMap<String, Integer>();
			Map<String, Set<Id>> undefinedLegAgents = new HashMap<String, Set<Id>>();
			for (String transportMode : transportModes) {
				int participatingAgents = 0;
				int notParticipatingAgents = 0;
				Set<Id> undefinedAgents = new HashSet<Id>();
				for (Id legAgentId : legAgentsInEvacuationArea.get(transportMode)) {
					Participating participating = this.decisionDataProvider.getPersonDecisionData(legAgentId).getParticipating();
					if (participating == Participating.TRUE) participatingAgents++;
					else if (participating == Participating.FALSE) notParticipatingAgents++;
					else if (participating == Participating.UNDEFINED) undefinedAgents.add(legAgentId);
					else throw new RuntimeException("Undefined participation state found: " + participating.toString());
				}
				participatingLegAgents.put(transportMode, participatingAgents);
				notParticipatingLegAgents.put(transportMode, notParticipatingAgents);
				undefinedLegAgents.put(transportMode, undefinedAgents);
			}
			
			// for all not processed past bins
			for (int index = currentBin; index < binIndex; index++) {
				activityBins[index] = (int) Math.round(activityAgentsInEvacuationArea.size() * scaleFactor);
				activityBinsParticipating[index] = (int) Math.round(participatingActivityAgents * scaleFactor);
				activityBinsNotParticipating[index] = (int) Math.round(notParticipatingActivityAgents * scaleFactor);
				undefinedActivityAgentBins.put(index, undefinedActivityAgents);
				
				for (String transportMode : transportModes) {
					legBins.get(transportMode)[index] = (int) Math.round(legAgentsInEvacuationArea.get(transportMode).size() * scaleFactor);
					legBinsParticipating.get(transportMode)[index] = (int) Math.round(participatingLegAgents.get(transportMode) * scaleFactor);
					legBinsNotParticipating.get(transportMode)[index] = (int) Math.round(notParticipatingLegAgents.get(transportMode) * scaleFactor);
				}
				undefinedLegAgentBins.put(index, undefinedLegAgents);
			}

		}

		currentBin = binIndex;
	}
	
	private void postProcessUndefinedAgents() {
		
		Set<Integer> bins = undefinedActivityAgentBins.keySet();
		for (int index : bins) {
			
			int participatingActivityAgents = 0;
			int notParticipatingActivityAgents = 0;
			for (Id activityAgentId : undefinedActivityAgentBins.get(index)) {
				Participating participating = this.decisionDataProvider.getPersonDecisionData(activityAgentId).getParticipating();
				if (participating == Participating.TRUE) participatingActivityAgents++;
				else if (participating == Participating.FALSE) notParticipatingActivityAgents++;
				else if (participating == Participating.UNDEFINED) {
					throw new RuntimeException("Participation state still UNDEFINED: " + activityAgentId.toString());
				}
				else throw new RuntimeException("Unexpected participation state found: " + participating.toString());
			}

			// update activity bin arrays
			activityBinsParticipating[index] = activityBinsParticipating[index] + (int) Math.round(participatingActivityAgents * scaleFactor);
			activityBinsNotParticipating[index] = activityBinsNotParticipating[index] + (int) Math.round(notParticipatingActivityAgents * scaleFactor);
			
			for (String transportMode : transportModes) {
				
				int participatingLegAgents = 0;
				int notParticipatingLegAgents = 0;
				for (Id legAgentId : undefinedLegAgentBins.get(index).get(transportMode)) {
					Participating participating = this.decisionDataProvider.getPersonDecisionData(legAgentId).getParticipating();
					if (participating == Participating.TRUE) participatingLegAgents++;
					else if (participating == Participating.FALSE) notParticipatingLegAgents++;
					else if (participating == Participating.UNDEFINED) {
						throw new RuntimeException("Participation state still UNDEFINED: " + legAgentId.toString());
					}
					else throw new RuntimeException("Unexpected participation state found: " + participating.toString());
				}
				
				// update leg bin arrays
				legBinsParticipating.get(transportMode)[index] = legBinsParticipating.get(transportMode)[index] +
						(int) Math.round(participatingLegAgents * scaleFactor);
				legBinsNotParticipating.get(transportMode)[index] = legBinsNotParticipating.get(transportMode)[index] + 
						(int) Math.round(notParticipatingLegAgents * scaleFactor);
			}			
		}
	}
}