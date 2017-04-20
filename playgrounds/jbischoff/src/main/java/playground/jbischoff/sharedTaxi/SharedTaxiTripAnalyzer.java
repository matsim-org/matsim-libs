/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.sharedTaxi;

import java.io.*;
import java.util.*;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.taxibus.algorithm.utils.TaxibusUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author jbischoff
 *
 */
public class SharedTaxiTripAnalyzer
		implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

	private Map<Id<Vehicle>, List<Id<Person>>> vehicles2pax;
	private Map<Id<Person>, Double> departureTime;
	private Map<Id<Person>, Double> enterTime;
	private List<String> ridesLog;
	private List<String> ridesPerIteration = new ArrayList<>();
	private Map<Id<Person>, Integer> paxPerRide;
	private EventsManager events;
	private SharedTaxiConfigGroup stt;

	private double totalWaittime = 0;
	private double totalRidetime = 0;
	private double totalFares = 0;
	private int totalRides = 0;

	// trip duration, (distance), waiting time, indication whether it is shared
	// or not, indicate whether it is first or second trip shared

	@Inject
	SharedTaxiTripAnalyzer(OutputDirectoryHierarchy controlerIO, EventsManager events, Config config) {
		reset(0);
		events.addHandler(this);
		this.events = events;
		stt = (SharedTaxiConfigGroup) config.getModules().get(SharedTaxiConfigGroup.GROUP_NAME);
		
		
	}

	@Override
	public void reset(int iteration) {
		this.vehicles2pax = new HashMap<>();
		this.departureTime = new HashMap<>();
		this.enterTime = new HashMap<>();
		this.ridesLog = new ArrayList<>();
		this.paxPerRide = new HashMap<>();
		totalWaittime = 0;
		totalRidetime = 0;
		totalRides = 0;
		totalFares = 0;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (this.departureTime.containsKey(event.getPersonId())) {
			if (this.enterTime.containsKey(event.getPersonId())) {
				double departureTime = this.departureTime.remove(event.getPersonId());
				double enterTime = this.enterTime.remove(event.getPersonId());
				double arrivalTime = event.getTime();
				double rideTime = arrivalTime - enterTime;
				double waitTime = enterTime - departureTime;
				int paxOnBoard = this.paxPerRide.remove(event.getPersonId());
				String row = (int) departureTime + ";" + event.getPersonId().toString() + ";" + (int) enterTime + ";"
						+ (int) arrivalTime + ";" + (int) waitTime + ";" + (int) rideTime+";"+paxOnBoard;
				this.ridesLog.add(row);
				
				if (stt.isScoreRides()){
					double discountFactor = (paxOnBoard>1)?stt.getDiscountForSharing():0;
					double fullfare = stt.getHourlyTaxiFare()*rideTime/3600;
					double discountedFare = fullfare - discountFactor*fullfare;
					events.processEvent(new PersonMoneyEvent(arrivalTime, event.getPersonId(), -discountedFare));
					totalFares+=discountedFare;
				}
				
				
				this.totalRides++;
				this.totalRidetime += rideTime;
				this.totalWaittime += waitTime;
			}

		}

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TaxibusUtils.TAXIBUS_MODE)) {
			this.departureTime.put(event.getPersonId(), event.getTime());
		}

	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (isTrackedVehicle(event.getVehicleId())) {
			if (!isDriverAgent(event.getPersonId(), event.getVehicleId())) {
				 this.vehicles2pax.get(event.getVehicleId()).remove(event.getPersonId());
				
				
			}
		}

	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (isTrackedVehicle(event.getVehicleId())) {
			if (!isDriverAgent(event.getPersonId(), event.getVehicleId())) {
				List <Id<Person>> paxOnBoard = this.vehicles2pax.get(event.getVehicleId());
				 paxOnBoard.add(event.getPersonId());
				 int paxno = paxOnBoard.size();
				 for (Id<Person> pax : paxOnBoard){
					 this.paxPerRide.put(pax, paxno);
				 }
				this.enterTime.put(event.getPersonId(), event.getTime());
			}
		}

	}

	private boolean isTrackedVehicle(Id<Vehicle> vid) {
		return this.vehicles2pax.containsKey(vid);
	}

	private boolean isDriverAgent(Id<Person> pid, Id<Vehicle> vehicleId) {
		Id<Vehicle> vid = Id.createVehicleId(pid.toString());
		if (vid.equals(vehicleId)) {
			return true;
		} else
			return false;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().startsWith(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			this.vehicles2pax.put(Id.createVehicleId(event.getPersonId().toString()), new ArrayList<Id<Person>>());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().startsWith(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE)) {
			this.vehicles2pax.remove(Id.createVehicleId(event.getPersonId().toString()));
		}

	}

	void writeStats(String outputFile) {
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try {

			bw.write("DepartureTime;Person;EnterTime;ArrivalTime;WaitTime;RideTime;MaxPaxOnBoard");
			for (String row : this.ridesLog) {
				bw.newLine();
				bw.write(row);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	void writeAverageStats(String outputFile) {
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try {
			bw.write("Iteration;TotalRides;AverageWait;AverageRideTime;TotalFares");
			for (String row : this.ridesPerIteration) {
				bw.newLine();
				bw.write(row);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	void aggregateRideTimes(int iteration) {
		String row = iteration + ";" + this.totalRides + ";" + this.totalWaittime / this.totalRides + ";"
				+ this.totalRidetime / this.totalRides + ";"+ this.totalFares; 
		this.ridesPerIteration.add(row);
	}

}
