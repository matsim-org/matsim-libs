/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package vwExamples.utils.customEdrtModule;

import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.data.Charger;
import org.matsim.contrib.ev.data.ChargingInfrastructure;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.network.NetworkUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author axer
 */
public class GetBestDepot implements DepotFinder {
	private final Set<Link> chargerLinks = new HashSet<>();
	private final Map<Link, MutableInt> CurrentHubCapacityMap;
	private final Map<Link, MutableInt> InitalHubCapacityMap;
	private final Map<Charger, Integer> ChargerQueueMap;
	private final ChargingInfrastructure chargingInfrastructure;
	private final Fleet fleet;
	private final ElectricFleet electricFleet;

	public GetBestDepot(ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet, Fleet fleet) {
		for (Charger c : chargingInfrastructure.getChargers().values()) {
			chargerLinks.add(c.getLink());
		}
		this.electricFleet = electricFleet;
		this.fleet = fleet;
		this.ChargerQueueMap = new HashMap<>();
		this.CurrentHubCapacityMap = new HashMap<>();
		this.InitalHubCapacityMap = new HashMap<>();
		this.chargingInfrastructure = chargingInfrastructure;
		
		//Links and allowed number of vehicle per hub
		//getInitalHubCapacity analysis the start_link of each vehicle
		getInitalHubCapacity();

	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra
	// should be the ultimate solution
	@Override
	public Link findDepot(DvrpVehicle vehicle) {
		
		ElectricVehicle ev = electricFleet.getElectricVehicles().get(vehicle.getId());
		double SOC = ev.getBattery().getSoc();
		
		Battery b = ev.getBattery();
		double remain =  b.getCapacity() - b.getSoc();
		
		return getBestCharger(vehicle);
	}

	public void updateChargerQueueMap() {
		// Get an update of the actual queuing situation at each charger
		this.ChargerQueueMap.clear();

		for (Entry<Id<Charger>, Charger> entry : chargingInfrastructure.getChargers().entrySet()) {

			int queuedVehicles = entry.getValue().getLogic().getQueuedVehicles().size();
			ChargerQueueMap.put(entry.getValue(), queuedVehicles);
		}

	}

	public boolean isIdleVehicleAtHub(DvrpVehicle vehicle) {

		ElectricVehicle ev = electricFleet.getElectricVehicles().get(vehicle.getId());

		// Is vehicle in hub located?
		DrtStayTask currentTask = (DrtStayTask) vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();

		boolean isLocatedinHub = InitalHubCapacityMap.containsKey(currentLink);

		// Vehicle is on hub
		if (isLocatedinHub) {
			// Check if vehicle is at charger or at queue of an charger

			Set<Entry<Id<Charger>, Charger>> chargerEntrys = chargingInfrastructure
					.getChargersAtLink(currentLink.getId()).entrySet();

			for (Entry<Id<Charger>, Charger> charger : chargerEntrys) {

				boolean vehicleAtQueue = charger.getValue().getLogic().getQueuedVehicles().contains(ev);
				boolean vehicleAtCharger = charger.getValue().getLogic().getPluggedVehicles().contains(ev);

				// If we see a vehicle at Queue or Charger it is not idle!
				if (vehicleAtQueue == true || vehicleAtCharger == true) {
					return false;

				}

			}
			// If loop has reached this point, we know it is not charging nor queued
			return true;

		}

		else {
			return false;
		}

	}

	public void checkIdleVehiclesAtHubs() {
		// Get an update of the actual hub situation
		// How many vehicles are in stay at a hub
		this.CurrentHubCapacityMap.clear();

		for (Entry<Id<DvrpVehicle>, ? extends DvrpVehicle> vEntry : fleet.getVehicles().entrySet()) {

			Task currentTask = vEntry.getValue().getSchedule().getCurrentTask();

			if (currentTask instanceof DrtStayTask) {

				DrtStayTask currentStayTask = (DrtStayTask) vEntry.getValue().getSchedule().getCurrentTask();
				Link currentLink = currentStayTask.getLink();

				// Is vehicle a charing or queuing vehicle
				boolean isIdleAtHub = isIdleVehicleAtHub(vEntry.getValue());

				if (isIdleAtHub) {
					if (CurrentHubCapacityMap.containsKey(currentLink)) {

						CurrentHubCapacityMap.get(currentLink).increment();
					} else {

						CurrentHubCapacityMap.put(currentLink, new MutableInt(0));
					}

				}

			}

		}

	}

	// Necessary to check maximum allowed Capacity
	public void getInitalHubCapacity() {
		// Get an update of the actual hub situation
		// How many vehicles are in stay at a hub
		this.InitalHubCapacityMap.clear();

		for (Entry<Id<DvrpVehicle>, ? extends DvrpVehicle> vEntry : fleet.getVehicles().entrySet()) {

			Link vehicleStartLink = vEntry.getValue().getStartLink();
			if (InitalHubCapacityMap.containsKey(vehicleStartLink)) {

				InitalHubCapacityMap.get(vehicleStartLink).increment();
			} else {

				InitalHubCapacityMap.put(vehicleStartLink, new MutableInt(0));
			}

		}

	}

	public Link getBestCharger(DvrpVehicle vehicle) {

		updateChargerQueueMap();

		checkIdleVehiclesAtHubs();
		
//		System.out.println("-----------------------------------------------------");
//		for (Entry<Link, MutableInt> hubEntry : CurrentHubCapacityMap.entrySet())
//		{
//			System.out.println(hubEntry.getKey().getId()+": "+hubEntry.getValue());
//			
//		}
//		System.out.println("-----------------------------------------------------");

		Map<Charger, Double> ChargerDistanceMap = new HashMap<>();
		Charger bestCharger = null;

		DrtStayTask currentTask = (DrtStayTask) vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();

		// Calculate Distance to Charger
		for (Charger charger : ChargerQueueMap.keySet()) {
			double approxDistanceToCharger = NetworkUtils.getEuclideanDistance(currentLink.getCoord(),
					charger.getLink().getCoord()) * 1.4;
			ChargerDistanceMap.put(charger, approxDistanceToCharger);
		}

		// Do we have charger with no Queue
		Map<Charger, Integer> chagersWithNoQueue = filterByValue(ChargerQueueMap, value -> value == 0);
		if (chagersWithNoQueue.size() > 0) {
			double distance = Double.MAX_VALUE;
			for (Charger charger : chagersWithNoQueue.keySet()) {

				double distanceToCharger = ChargerDistanceMap.get(charger);
				if (distanceToCharger < distance) {
					bestCharger = charger;
					distance = distanceToCharger;
				}

			}
			// System.out.println("None queued chargers available, distance to hub: " +
			// distance);
			return bestCharger.getLink();

			// All Chargers have Queues, we use the charger with the lowest queue
		} else

		{
			int queue = Integer.MAX_VALUE;

			for (Charger charger : ChargerQueueMap.keySet()) {

				int queueAtCharger = ChargerQueueMap.get(charger);

				if (queueAtCharger < queue) {
					bestCharger = charger;
					queue = queueAtCharger;
				}
			}
			// System.out.println("Only queued chargers available, min queue at charger: " +
			// queue);
			return bestCharger.getLink();

		}

	}

	public Link getHubForIdleTime(DvrpVehicle vehicle) {

		updateChargerQueueMap();
		// updateCurrentHubCapacityMap();
		Map<Charger, Double> ChargerDistanceMap = new HashMap<>();
		Charger bestCharger = null;

		DrtStayTask currentTask = (DrtStayTask) vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();

		// Calculate Distance to Charger
		for (Charger charger : ChargerQueueMap.keySet()) {
			double approxDistanceToCharger = NetworkUtils.getEuclideanDistance(currentLink.getCoord(),
					charger.getLink().getCoord()) * 1.4;
			ChargerDistanceMap.put(charger, approxDistanceToCharger);
		}

		// Do we have charger with no Queue
		Map<Charger, Integer> chagersWithNoQueue = filterByValue(ChargerQueueMap, value -> value == 0);
		if (chagersWithNoQueue.size() > 0) {
			double distance = Double.MAX_VALUE;
			for (Charger charger : chagersWithNoQueue.keySet()) {

				double distanceToCharger = ChargerDistanceMap.get(charger);
				if (distanceToCharger < distance) {
					bestCharger = charger;
					distance = distanceToCharger;
				}

			}
			// System.out.println("None queued chargers available, distance to hub: " +
			// distance);
			return bestCharger.getLink();

			// All Chargers have Queues, we use the charger with the lowest queue
		} else

		{
			int queue = Integer.MAX_VALUE;

			for (Charger charger : ChargerQueueMap.keySet()) {

				int queueAtCharger = ChargerQueueMap.get(charger);

				if (queueAtCharger < queue) {
					bestCharger = charger;
					queue = queueAtCharger;
				}
			}
			// System.out.println("Only queued chargers available, min queue at charger: " +
			// queue);
			return bestCharger.getLink();

		}

	}

	static <K, V> Map<K, V> filterByValue(Map<K, V> map, Predicate<V> predicate) {
		return map.entrySet().stream().filter(entry -> predicate.test(entry.getValue()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

}