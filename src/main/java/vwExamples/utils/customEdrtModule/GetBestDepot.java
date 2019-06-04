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

import java.util.Collections;
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
	private Map<Link, MutableInt> CurrentHubCapacityMap;
	private Map<Link, MutableInt> InitalHubCapacityMap;
	private final Map<Charger, Integer> ChargerQueueMap;
	private final Map<Charger, Integer> ChargerChargeMap;
	private final ChargingInfrastructure chargingInfrastructure;
	private final Fleet fleet;
	private final ElectricFleet electricFleet;
	private final Map<DvrpVehicle,Id<Link>> vehicleHubMap;

	public GetBestDepot(ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet, Fleet fleet) {
		for (Charger c : chargingInfrastructure.getChargers().values()) {
			chargerLinks.add(c.getLink());
		}
		this.electricFleet = electricFleet;
		this.fleet = fleet;
		this.ChargerQueueMap = new HashMap<>();
		this.ChargerChargeMap = new HashMap<>();
		this.CurrentHubCapacityMap = new HashMap<>();
		this.InitalHubCapacityMap = new HashMap<>();
		this.chargingInfrastructure = chargingInfrastructure;
		this.vehicleHubMap = new HashMap<>();

		getInitalHubCapacity();
		setMinlHubCapacity();
	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra
	// should be the ultimate solution
	@Override
	public Link findDepot(DvrpVehicle vehicle) {
		Id<ElectricVehicle> evId = Id.create(vehicle.getId(), ElectricVehicle.class);
		ElectricVehicle ev = electricFleet.getElectricVehicles().get(evId);
		Battery b = ev.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();

//		return getBestHubForIdleTime(vehicle);
//		// These vehicles are searching for a charger
		if (relativeSoc < 0.5) {
			return getBestCharger(vehicle);

			// These vehicles are searching for a hub to get idle
		} else
			return getBestHubForIdleTime(vehicle);

	}

	public void updateChargerMap() {
		// Get an update of the actual queuing situation at each charger
		this.ChargerQueueMap.clear();
		this.ChargerChargeMap.clear();

		for (Entry<Id<Charger>, Charger> entry : chargingInfrastructure.getChargers().entrySet()) {

			int queuedVehicles = entry.getValue().getLogic().getQueuedVehicles().size();
			ChargerQueueMap.put(entry.getValue(), queuedVehicles);
			
			int chargedVehicles = entry.getValue().getLogic().getPluggedVehicles().size();
			ChargerChargeMap.put(entry.getValue(), queuedVehicles);
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
//				System.out.print(vehicleAtQueue + " || " + vehicleAtCharger +"\n");
				// If we see a vehicle at Queue or Charger it is not idle!
				if ((vehicleAtQueue == true) || (vehicleAtCharger == true)) {
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

	public void currentHubCapacity() {
		// Get an update of the actual hub situation
		// How many vehicles are in stay at a hub

		// Links and allowed number of vehicle per hub
		// getInitalHubCapacity analysis the start_link of each vehicle
		// getInitalHubCapacity();

		this.CurrentHubCapacityMap.clear();
		// this.CurrentHubCapacityMap.putAll(InitalHubCapacityMap);

		//Copy values of InitalHubCapacityMap to CurrentHubCapacityMap
		for (Entry<Link, MutableInt> intitalHubEntry : InitalHubCapacityMap.entrySet()) {
			CurrentHubCapacityMap.put(intitalHubEntry.getKey(), new MutableInt(intitalHubEntry.getValue()) );

		}

		for (Entry<Id<DvrpVehicle>, ? extends DvrpVehicle> vEntry : fleet.getVehicles().entrySet()) {

			Task currentTask = vEntry.getValue().getSchedule().getCurrentTask();

			if (currentTask instanceof DrtStayTask) {

				DrtStayTask currentStayTask = (DrtStayTask) vEntry.getValue().getSchedule().getCurrentTask();
				Link currentLink = currentStayTask.getLink();

				// Is vehicle a charing or queuing vehicle
				boolean isIdleAtHub = isIdleVehicleAtHub(vEntry.getValue());

				if (isIdleAtHub) {
					if (CurrentHubCapacityMap.containsKey(currentLink)) {

						CurrentHubCapacityMap.get(currentLink).decrement();
						
						
						//Each vehicles which stays 
						if (vehicleHubMap.containsKey(vEntry.getValue()))
						{
							
							vehicleHubMap.remove(vEntry.getValue());
//							System.out.println("Unregisterd vehicle: " + vEntry.getKey()  + "@ " +currentLink.getId());
							
						}
					}
					// else {
					// // Initialize Hub with InitalHubCapacityMap
					// CurrentHubCapacityMap.put(currentLink, new
					// MutableInt(InitalHubCapacityMap.get(currentLink)));
					// CurrentHubCapacityMap.get(currentLink).decrement();
					// }

				}

			}

		}

	}

	// Necessary to check maximum allowed Capacity
	public void getInitalHubCapacity() {
		// Get an update of the actual hub situation
		// How many vehicles are in stay at a hub
		this.InitalHubCapacityMap.clear();

		for (Entry<Id<DvrpVehicle>, ? extends DvrpVehicle> vEntry : this.fleet.getVehicles().entrySet()) {

			Link vehicleStartLink = vEntry.getValue().getStartLink();
			if (this.InitalHubCapacityMap.containsKey(vehicleStartLink)) {

				this.InitalHubCapacityMap.get(vehicleStartLink).increment();
			} else {

				this.InitalHubCapacityMap.put(vehicleStartLink, new MutableInt(1));
			}

		}

	}
	
	
	public void setMinlHubCapacity() {
		int minHubCapacity=100;
		// Get an update of the actual hub situation
		// How many vehicles are in stay at a hub
//		this.InitalHubCapacityMap.clear();

		for (Entry<Link, MutableInt> hubEntry : InitalHubCapacityMap.entrySet())
		{
			if (hubEntry.getValue().toInteger() == 1)
			{
				System.out.println("Hub " + hubEntry.getKey().getId() + " with one inital vehicle detected");
				hubEntry.getValue().setValue(minHubCapacity);
				System.out.println("Set capacity to: " +hubEntry.getValue().intValue());
				
			}
			
		}

	}

	public Link getBestHubForIdleTime(DvrpVehicle vehicle) {

		currentHubCapacity();

//		int i=0;
//		System.out.println("-----------------------------------------------------");
//		for (Entry<Link, MutableInt> hubEntry : CurrentHubCapacityMap.entrySet()) {
//			i++;
//			System.out.println(i+": "+ hubEntry.getKey().getId() + ": " + hubEntry.getValue());
//
//		}
//		System.out.println("-----------------------------------------------------");

		Map<Link, Double> HubDistanceMap = new HashMap<>();
		Link bestHub = null;

		DrtStayTask currentTask = (DrtStayTask) vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();

		// Calculate Distance to Hub
		for (Link hubLink : InitalHubCapacityMap.keySet()) {
			double approxDistanceToHub = NetworkUtils.getEuclideanDistance(currentLink.getCoord(), hubLink.getCoord())
					* 1.4;
			HubDistanceMap.put(hubLink, approxDistanceToHub);
		}

		// Filter hubs with remaining capacity
		// Select the nearest free hub
		Map<Link, MutableInt> HubsWithRemaingCapacity = new HashMap<>();

//		HubsWithRemaingCapacity.putAll(CurrentHubCapacityMap);
//		System.out.println("-------------");
		//Copy values of InitalHubCapacityMap to CurrentHubCapacityMap
		for (Entry<Link, MutableInt> currentHubEntry : CurrentHubCapacityMap.entrySet()) {
			
			int assignedVehicle = Collections.frequency(vehicleHubMap.values(), currentHubEntry.getKey().getId());
						
			int currentHubCapa = currentHubEntry.getValue().intValue();
			
//			System.out.println(currentHubEntry.getKey().getId()+" Capacity: "+currentHubCapa + " ||" +" Assigned: "+assignedVehicle + " ||" +" Remain: "+(currentHubCapa-assignedVehicle));
			
//			HubsWithRemaingCapacity.put(currentHubEntry.getKey(), new MutableInt(currentHubEntry.getValue()) );
			HubsWithRemaingCapacity.put(currentHubEntry.getKey(), new MutableInt(currentHubCapa - assignedVehicle ) );
		}
//		System.out.println("-------------");
		

		HubsWithRemaingCapacity = filterByValue(HubsWithRemaingCapacity, value -> value.intValue() > 0);

		if (HubsWithRemaingCapacity.size() > 0) {
			double distance = Double.MAX_VALUE;
			for (Link hub : HubsWithRemaingCapacity.keySet()) {

				double distanceToHub = HubDistanceMap.get(hub);
				if (distanceToHub < distance) {
					bestHub = hub;
					distance = distanceToHub;
				}

			}
			
			
			//Each vehicle that is send to a hub gets registered in the assignedVehicles map
			
			if (vehicleHubMap.containsKey(vehicle))
			{
//				System.out.println("Remove registered vehicle " + vehicle.getId() + " to allow new assignment " +bestHub.getId());
				vehicleHubMap.remove(vehicle);
			} else {
			
			vehicleHubMap.put(vehicle, bestHub.getId());
						
//			System.out.println("Registered vehicle: " + vehicle.getId() + "@ " +bestHub.getId());
			}
			
			

			return bestHub;

		}

		else {
			throw new RuntimeException("Overall hub capacity != Fleet size");
		}

	}

	public Link getBestCharger(DvrpVehicle vehicle) {

		updateChargerMap();

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
		Map<Charger, Integer> chagersWithNoQueue =  new HashMap<>(); 
		
		//Copy values
		for (Entry<Charger, Integer> ChargerQueueMapEntry : ChargerQueueMap.entrySet()) {
			chagersWithNoQueue.put(ChargerQueueMapEntry.getKey(),ChargerQueueMapEntry.getValue() );
		}
		
		
		chagersWithNoQueue = filterByValue(chagersWithNoQueue, value -> value == 0);
		
		if (chagersWithNoQueue.size() > 0) {
			double distance = Double.MAX_VALUE;
			for (Charger charger : chagersWithNoQueue.keySet()) {

				double distanceToCharger = ChargerDistanceMap.get(charger);
				if (distanceToCharger < distance) {
					bestCharger = charger;
					distance = distanceToCharger;
				}

			}
//			 System.out.println("None queued charger:" + bestCharger.getId() + " available, distance to hub: " +
//			 distance);
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