/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch.evaluation;

import com.google.inject.Inject;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class ParkingSlotVisualiser implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler, IterationEndsListener {

	Network network;

	protected Map<Id<Link>, ParkingSlotManager> slotsOnLink = new HashMap<Id<Link>, ParkingSlotManager>();
	protected Map<Id<Vehicle>, Double> midnightParkers = new HashMap<Id<Vehicle>, Double>();
	protected Map<Id<Vehicle>, ParkingSlotManager> vehiclesResponsibleManager = new HashMap<>();
	Random r = MatsimRandom.getLocalInstance();
	protected List<String> parkings = new ArrayList<>();

	protected Map<Id<Vehicle>, Id<Link>> parkedVehicles = new HashMap<Id<Vehicle>, Id<Link>>();

	/**
	 *
	 */
	@Inject
	public ParkingSlotVisualiser(Scenario scenario) {
		this.network = scenario.getNetwork();
		Map<Id<ActivityFacility>, ActivityFacility> parkingFacilities = scenario.getActivityFacilities().getFacilitiesForActivityType(
			ParkingUtils.ParkingStageInteractionType);
		initialize(parkingFacilities);
	}

	public ParkingSlotVisualiser(Network network, Map<Id<ActivityFacility>, ActivityFacility> parkingFacilities) {
		this.network = network;
		initialize(parkingFacilities);
	}

	private void initialize(Map<Id<ActivityFacility>, ActivityFacility> parkingFacilities) {
		Map<Id<Link>, MutableDouble> nrOfSlotsPerLink = new HashMap<Id<Link>, MutableDouble>();
		for (ActivityFacility fac : parkingFacilities.values()) {
			Id<Link> linkId = fac.getLinkId();
			if (nrOfSlotsPerLink.containsKey(linkId)) {
				nrOfSlotsPerLink.get(linkId).add(fac.getActivityOptions().get(ParkingUtils.ParkingStageInteractionType).getCapacity());
			} else {
				nrOfSlotsPerLink.put(linkId, new MutableDouble(fac.getActivityOptions().get(ParkingUtils.ParkingStageInteractionType).getCapacity()));
			}
		}

		for (Id<Link> linkID : nrOfSlotsPerLink.keySet()) {
//			LogManager.getLogger(getClass()).info("initialize parking visualisation for link " + linkID);
			this.slotsOnLink.put(linkID, new ParkingSlotManager(network.getLinks().get(linkID), nrOfSlotsPerLink.get(linkID).intValue()));
		}
	}


	@Override
	public void reset(int iteration) {
		for (Id<Link> link : this.slotsOnLink.keySet()) {
			this.slotsOnLink.get(link).setAllParkingTimesToZero();
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (this.slotsOnLink.containsKey(event.getLinkId())) {
			this.vehiclesResponsibleManager.put(event.getVehicleId(), this.slotsOnLink.get(event.getLinkId()));
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		ParkingSlotManager manager = this.vehiclesResponsibleManager.remove(event.getVehicleId());
		if (manager != null) {
			Tuple<Coord, Double> parkingTuple = manager.processParking(event.getTime(), event.getVehicleId());
			this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + event.getTime() + ";" +
				parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "free");
			this.parkedVehicles.put(event.getVehicleId(), manager.getLinkId());
		}
	}


	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.parkedVehicles.containsKey(event.getVehicleId())) {
			ParkingSlotManager manager = this.slotsOnLink.get(this.parkedVehicles.get(event.getVehicleId()));
			Tuple<Coord, Double> parkingTuple = manager.processUnParking(event.getTime(), event.getVehicleId());
			this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + event.getTime() + ";" +
				parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "veh" + event.getVehicleId());
			this.parkedVehicles.remove(event.getVehicleId());
		} else {
			midnightParkers.put(event.getVehicleId(), event.getTime());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler#handleEvent(org.matsim.api.core.v01.events.VehicleEntersTrafficEvent)
	 */
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (this.midnightParkers.containsKey(event.getVehicleId())) {
			if (this.slotsOnLink.containsKey(event.getLinkId())) {
				ParkingSlotManager manager = this.slotsOnLink.get(event.getLinkId());
				Tuple<Coord, Double> parkingTuple = manager.processUnParking(event.getTime(), event.getVehicleId());
				if (parkingTuple != null) {
					this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + event.getTime() + ";" +
						parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "veh" + event.getVehicleId());
				}
			}
			this.midnightParkers.remove(event.getVehicleId());
		}
	}

	public void finishDay() {

		for (Id<Link> linkId : this.slotsOnLink.keySet()) {
			ParkingSlotManager manager = this.slotsOnLink.get(linkId);
			Map<Id<Vehicle>, Tuple<Coord, Double>> occupiedSlots = manager.getOccupiedSlots();

			double endOfDay = 30 * 3600;
			for (Entry<Id<Vehicle>, Tuple<Coord, Double>> e : occupiedSlots.entrySet()) {
				Tuple<Coord, Double> parkingTuple = e.getValue();
				this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + endOfDay + ";" +
					parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "veh" + e.getKey());

				// set back to 0
			}

			List<Tuple<Coord, Double>> freeSlots = manager.getFreeSlots();
			for (Tuple<Coord, Double> parkingTuple : freeSlots) {
				this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + endOfDay + ";" +
					parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "free");
			}
		}
	}

	public void plotSlotOccupation(String filename) {
		String head = "LinkId;from;To;X;Y;OccupiedByVehicle";
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write(head);
			for (String s : this.parkings) {
				bw.newLine();
				bw.write(s);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		LogManager.getLogger(getClass()).info("FINISHED WRITING PARKING SLOT VISUALISATION FILE TO: " + filename);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String path = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
			"ParkingSlots_it" + event.getIteration() + ".csv");
		this.finishDay();
		this.plotSlotOccupation(path);
	}
}

