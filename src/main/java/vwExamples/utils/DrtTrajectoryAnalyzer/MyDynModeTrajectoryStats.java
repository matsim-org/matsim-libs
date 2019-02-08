/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.utils.DrtTrajectoryAnalyzer;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.DynModeTripsAnalyser;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.data.ElectricFleet;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import java.util.*;


/**
 * @author saxer
 */
public class MyDynModeTrajectoryStats
		implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,
		LinkLeaveEventHandler,  ActivityEndEventHandler, DrtRequestSubmittedEventHandler,
		PassengerRequestRejectedEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, ActivityStartEventHandler {

	private final Map<Id<Person>, Double> departureTimes = new HashMap<>();
	public final Map<Id<Vehicle>, List<String>> vehicleTrajectoryMap = new HashMap<>();
	private final  Set<Id<Vehicle>> activeVehicle = new HashSet<>();
	private final Map<Id<Person>, Id<Link>> departureLinks = new HashMap<>();
	private final List<DynModeTrip> drtTrips = new ArrayList<>();
	private final Map<Id<Vehicle>, Map<Id<Person>, MutableDouble>> inVehicleDistance = new HashMap<>();
	private final Map<Id<Vehicle>, double[]> vehicleDistances = new HashMap<>();
	private final Map<Id<Person>, DynModeTrip> currentTrips = new HashMap<>();
	private final Map<Id<Person>, Double> unsharedDistances = new HashMap<>();
	private final Map<Id<Person>, Double> unsharedTimes = new HashMap<>();
	private final Set<Id<Person>> rejectedPersons = new HashSet<>();
	private final Map<Id<Request>, Id<Person>> request2person = new HashMap<>();
	private final String mode;
	private final int maxcap;
	private final Network network;
	private final Fleet fleet;
	private final FleetSpecification fleetSpecification;
	private String sep = ",";
	private final ElectricFleet electricFleet;

	public MyDynModeTrajectoryStats(Network network, EventsManager events, DrtConfigGroup drtCfg,
									FleetSpecification fleetSpecification, Fleet fleet, ElectricFleet electricFleet) {
		this.fleetSpecification = fleetSpecification;
		this.fleet = fleet;
		this.mode = drtCfg.getMode();
		this.network = network;
		events.addHandler(this);
		maxcap = DynModeTripsAnalyser.findMaxVehicleCapacity(fleetSpecification);
		this.electricFleet = electricFleet;

	}

	private static class VehDrive {
		private final Id<Vehicle> vehicleId;
		private final DvrpVehicleSpecification veh;
		public double movedOverNodeTime;
		private final double distance_till_now;

		public VehDrive(Id<Vehicle> vehicleId, DvrpVehicleSpecification vehicle) {
			this.vehicleId = vehicleId;
			this.veh = vehicle;
			movedOverNodeTime = Double.NaN;
			distance_till_now = Double.NaN;
		}

		private boolean isOnFirstLink() {
			return Double.isNaN(movedOverNodeTime);
		}
	}

	private final Map<Id<Vehicle>, VehDrive> vehDrives = new HashMap<>();


	@Override
	public void reset(int iteration) {
		drtTrips.clear();
		departureTimes.clear();
		departureLinks.clear();
		inVehicleDistance.clear();
		currentTrips.clear();
		vehicleDistances.clear();
		unsharedDistances.clear();
		unsharedTimes.clear();
		rejectedPersons.clear();
		request2person.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());
			this.inVehicleDistance.put(vid, new HashMap<Id<Person>, MutableDouble>());
			this.vehicleDistances.put(vid, new double[3 + maxcap]);
		}


		if (event.getActType().equals("ChargingActivity"))
		{
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());

			double acutalTime = event.getTime();
			String vehicleID = vid.toString();
			double dist = this.vehicleDistances.get(vid)[0];
			int occupancy = inVehicleDistance.get(vid).size();
			String actualTaskType = "CHARGE_END";
			double x = network.getLinks().get(event.getLinkId()).getCoord().getX();
			double y = network.getLinks().get(event.getLinkId()).getCoord().getY();

			double tt = Double.NaN;;
			double v_meterPerSec = Double.NaN;
			Id<ElectricVehicle> evId = Id.create(vid, ElectricVehicle.class);
			double currentSoc = EvUnits.J_to_kWh(electricFleet.getElectricVehicles().get(evId).getBattery().getSoc());
			String line = (vehicleID + sep + acutalTime + sep + occupancy + sep + dist + sep + actualTaskType + sep + x
					+ sep + y + sep + tt + sep + v_meterPerSec + sep + currentSoc);

			if (vehicleTrajectoryMap.containsKey(vid))
			{
				vehicleTrajectoryMap.get(vid).add(line);
			}
			else {
				//Add the first line to the list and initialize the list
				vehicleTrajectoryMap.put(vid, new ArrayList<>());
			}

		}

	}


	@Override
	public void handleEvent(ActivityStartEvent event) {



		if (event.getActType().equals("ChargingActivity"))
		{
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());

			double acutalTime = event.getTime();
			String vehicleID = vid.toString();
			double dist = this.vehicleDistances.get(vid)[0];
			int occupancy = inVehicleDistance.get(vid).size();
			String actualTaskType = "CHARGE_START";
			double x = network.getLinks().get(event.getLinkId()).getCoord().getX();
			double y = network.getLinks().get(event.getLinkId()).getCoord().getY();
			
			double tt = Double.NaN;;
			double v_meterPerSec = Double.NaN;
			Id<ElectricVehicle> evId = Id.create(vid, ElectricVehicle.class);
			double currentSoc = EvUnits.J_to_kWh(electricFleet.getElectricVehicles().get(evId).getBattery().getSoc());
			String line = (vehicleID + sep + acutalTime + sep + occupancy + sep + dist + sep + actualTaskType + sep + x + sep + y + sep + tt + sep + v_meterPerSec + sep + currentSoc);

			if (vehicleTrajectoryMap.containsKey(vid))
			{
				vehicleTrajectoryMap.get(vid).add(line);
			}
			else {
				//Add the first line to the list and initialize the list
				vehicleTrajectoryMap.put(vid, new ArrayList<>());
			}

		}



	}




	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		if (fleetSpecification.getVehicleSpecifications().containsKey(vehicleId)) {// handle all drt
			activeVehicle.add(vehicleId);


			vehDrives.put(vehicleId, new VehDrive(vehicleId, fleetSpecification.getVehicleSpecifications().get(vehicleId)));

			double acutalTime = event.getTime();
			String vehicleID = event.getVehicleId().toString();
			double dist = this.vehicleDistances.get(event.getVehicleId())[0];
			int occupancy = inVehicleDistance.get(event.getVehicleId()).size();
			double x = network.getLinks().get(event.getLinkId()).getCoord().getX();
			double y = network.getLinks().get(event.getLinkId()).getCoord().getY();

			double tt =  Double.NaN;
			DrtTask actualTask = (DrtTask) fleet.getVehicles().get(Id.create(vehicleID, DvrpVehicle.class)).getSchedule().getCurrentTask();
			String actualTaskType = actualTask.getDrtTaskType().toString();

			double v_meterPerSec = Double.NaN;
			Id<ElectricVehicle> evId = Id.create(event.getVehicleId(), ElectricVehicle.class);
			double currentSoc = EvUnits.J_to_kWh(electricFleet.getElectricVehicles().get(evId).getBattery().getSoc());
			String line = (vehicleID + sep + acutalTime + sep + occupancy + sep + dist + sep + actualTaskType + sep + x + sep + y + sep + tt + sep + v_meterPerSec + sep + currentSoc);


			if (vehicleTrajectoryMap.containsKey(event.getVehicleId()))
			{
				vehicleTrajectoryMap.get(event.getVehicleId()).add(line);
			}
			else {
				//Add the first line to the list and initialize the list
				vehicleTrajectoryMap.put(event.getVehicleId(), new ArrayList<>());
			}
		}

	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		if (fleetSpecification.getVehicleSpecifications().containsKey(vehicleId)) {// handle all drt
			activeVehicle.add(vehicleId);

			vehDrives.remove(vehicleId, new VehDrive(vehicleId, fleetSpecification.getVehicleSpecifications().get(vehicleId)));

			double acutalTime = event.getTime();
			String vehicleID = event.getVehicleId().toString();
			double dist = this.vehicleDistances.get(event.getVehicleId())[0];
			int occupancy = inVehicleDistance.get(event.getVehicleId()).size();

			double x = network.getLinks().get(event.getLinkId()).getCoord().getX();
			double y = network.getLinks().get(event.getLinkId()).getCoord().getY();

			double tt =  Double.NaN;

			double v_meterPerSec = Double.NaN;

			DrtTask actualTask = (DrtTask) fleet.getVehicles().get(Id.create(vehicleID, DvrpVehicle.class)).getSchedule().getCurrentTask();
			String actualTaskType = actualTask.getDrtTaskType().toString();

			Id<ElectricVehicle> evId = Id.create(event.getVehicleId(), ElectricVehicle.class);
			double currentSoc = EvUnits.J_to_kWh(electricFleet.getElectricVehicles().get(evId).getBattery().getSoc());
			String line = (vehicleID + sep + acutalTime + sep + occupancy + sep + dist + sep + actualTaskType + sep + x + sep + y + sep + tt + sep + v_meterPerSec + sep + currentSoc);


			if (vehicleTrajectoryMap.containsKey(event.getVehicleId()))
			{
				vehicleTrajectoryMap.get(event.getVehicleId()).add(line);
			}
			else {
				//Add the first line to the list and initialize the list
				vehicleTrajectoryMap.put(event.getVehicleId(), new ArrayList<>());
			}
		}

	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (inVehicleDistance.containsKey(event.getVehicleId())) {
			double distance = network.getLinks().get(event.getLinkId()).getLength();
			for (MutableDouble d : inVehicleDistance.get(event.getVehicleId()).values()) {
				d.add(distance);
			}

			this.vehicleDistances.get(event.getVehicleId())[0] += distance; // overall distance drive

			//Cumulative DvrpVehicle Distance, actual occupancy
			double acutalTime = event.getTime();
			String vehicleID = event.getVehicleId().toString();
			double dist = this.vehicleDistances.get(event.getVehicleId())[0];
			int occupancy = inVehicleDistance.get(event.getVehicleId()).size();

			double x = network.getLinks().get(event.getLinkId()).getCoord().getX();
			double y = network.getLinks().get(event.getLinkId()).getCoord().getY();

			//Assign first time stamp after having traversed the first link
			VehDrive vehDrive =  vehDrives.get(event.getVehicleId());
			double tt =  Double.NaN;

			if (!vehDrive.isOnFirstLink())
			{
				tt = event.getTime() - vehDrive.movedOverNodeTime;

			}

			if (vehDrive.isOnFirstLink())
			{
				double defaultSpeedOnFirstEdge = 7.0; //meter per second
				tt = network.getLinks().get(event.getLinkId()).getLength()/defaultSpeedOnFirstEdge;

			}

			vehDrive.movedOverNodeTime = event.getTime();
			double v_meterPerSec = distance / tt;

			DrtTask actualTask = (DrtTask) fleet.getVehicles().get(Id.create(vehicleID, DvrpVehicle.class)).getSchedule().getCurrentTask();
			String actualTaskType = actualTask.getDrtTaskType().toString();

			Id<ElectricVehicle> evId = Id.create(event.getVehicleId(), ElectricVehicle.class);
			double currentSoc = EvUnits.J_to_kWh(electricFleet.getElectricVehicles().get(evId).getBattery().getSoc());
			String line = (vehicleID + sep + acutalTime + sep + occupancy + sep + dist + sep + actualTaskType + sep + x + sep + y + sep + tt + sep + v_meterPerSec + sep + currentSoc);


			if (vehicleTrajectoryMap.containsKey(event.getVehicleId()))
			{
				vehicleTrajectoryMap.get(event.getVehicleId()).add(line);
			}
			else {
				//Add the first line to the list and initialize the list
				vehicleTrajectoryMap.put(event.getVehicleId(), new ArrayList<>());
			}

			this.vehicleDistances.get(event.getVehicleId())[1] += distance * occupancy; // overall revenue distance
			if (occupancy > 0) {
				this.vehicleDistances.get(event.getVehicleId())[2] += distance; // overall occupied distance
				this.vehicleDistances.get(event.getVehicleId())[2
						+ occupancy] += distance; // overall occupied distance with n passengers

			}
		}

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(mode)) {
			DynModeTrip trip = currentTrips.remove(event.getPersonId());
			if (trip != null) {
				double distance = inVehicleDistance.get(trip.getVehicle()).remove(event.getPersonId()).doubleValue();
				trip.setTravelDistance(distance);
				trip.setArrivalTime(event.getTime());
				trip.setToLink(event.getLinkId());
				Coord toCoord = this.network.getLinks().get(event.getLinkId()).getCoord();
				trip.setToCoord(toCoord);
				trip.setInVehicleTravelTime(event.getTime() - trip.getDepartureTime() - trip.getWaitTime());
			} else {
				if (this.rejectedPersons.contains(event.getPersonId())) {
					// XXX might happen only if we teleported rejected passengers
				} else {
					throw new NullPointerException("Arrival without departure?");
				}
			}

			// remove person in case the person has trips with dvrp and non-dvrp modes
			this.departureTimes.remove(event.getPersonId());
		}

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode)) {
			this.departureTimes.put(event.getPersonId(), event.getTime());
			this.departureLinks.put(event.getPersonId(), event.getLinkId());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.departureTimes.containsKey(event.getPersonId())) {
			double departureTime = this.departureTimes.remove(event.getPersonId());
			double waitTime = event.getTime() - departureTime;
			Id<Link> departureLink = this.departureLinks.remove(event.getPersonId());
			double unsharedDistance = this.unsharedDistances.remove(event.getPersonId());
			double unsharedTime = this.unsharedTimes.remove(event.getPersonId());
			Coord departureCoord = this.network.getLinks().get(departureLink).getCoord();
			DynModeTrip trip = new DynModeTrip(departureTime, event.getPersonId(), event.getVehicleId(), departureLink,
					departureCoord, waitTime);
			trip.setUnsharedDistanceEstimate_m(unsharedDistance);
			trip.setUnsharedTimeEstimate_m(unsharedTime);
			this.drtTrips.add(trip);
			this.currentTrips.put(event.getPersonId(), trip);
			this.inVehicleDistance.get(event.getVehicleId()).put(event.getPersonId(), new MutableDouble());
		}
	}

	/**
	 * @return the drtTrips
	 */
	public List<DynModeTrip> getDrtTrips() {
		return drtTrips;
	}

	/**
	 * @return the vehicleDistances
	 */
	public Map<Id<Vehicle>, double[]> getVehicleDistances() {
		return vehicleDistances;
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (!event.getMode().equals(mode)) {
			return;
		}
		this.unsharedDistances.put(event.getPersonId(), event.getUnsharedRideDistance());
		this.unsharedTimes.put(event.getPersonId(), event.getUnsharedRideTime());
		this.request2person.put(event.getRequestId(), event.getPersonId());
		this.rejectedPersons.remove(event.getPersonId());// XXX needed only if we teleported rejected passengers
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (!event.getMode().equals(mode)) {
			return;
		}
		rejectedPersons.add(request2person.get(event.getRequestId()));
	}
}
