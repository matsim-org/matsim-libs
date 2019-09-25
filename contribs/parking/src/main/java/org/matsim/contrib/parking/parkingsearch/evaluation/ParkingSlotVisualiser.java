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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.log4j.Logger;
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

import com.google.inject.Inject;

public class ParkingSlotVisualiser implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler, IterationEndsListener{
	
	Network network;

	Map<Id<Link>,ParkingSlotManager> slotsOnLink = new HashMap<Id<Link>,ParkingSlotManager>();
	Map<Id<Vehicle>,Tuple<Id<Link>,MutableDouble>> vehicleParkingPosition = new HashMap<>();
	Map<Id<Vehicle>,Double> midnightParkers = new HashMap<Id<Vehicle>,Double>();
	Map<Id<Vehicle>,ParkingSlotManager> vehiclesResponsibleManager = new HashMap<>();
	Random r = MatsimRandom.getLocalInstance();
	List<String> parkings = new ArrayList<>();
	
	Map<Id<Vehicle>,Id<Link>> parkedVehicles = new HashMap<Id<Vehicle>,Id<Link>>();
		
	/**
	 * 
	 */
	@Inject
	public ParkingSlotVisualiser(Scenario scenario) {
		this.network = scenario.getNetwork();
		Map<Id<org.matsim.facilities.Facility>, ActivityFacility> parkingFacilities = scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE);
		initialize(parkingFacilities);
	}
	
	public ParkingSlotVisualiser(Network network, Map<Id<org.matsim.facilities.Facility>, ActivityFacility> parkingFacilities) {
		this.network = network;
		initialize(parkingFacilities);
	}
	
	private void initialize(Map<Id<org.matsim.facilities.Facility>, ActivityFacility> parkingFacilities){
		Map<Id<Link>,MutableDouble> nrOfSlotsPerLink = new HashMap<Id<Link>,MutableDouble>();
		for (ActivityFacility fac : parkingFacilities.values()) {
			Id<Link> linkId = fac.getLinkId();
			if(nrOfSlotsPerLink.containsKey(linkId)){
				nrOfSlotsPerLink.get(linkId).add(fac.getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity());
			}
			else{
				nrOfSlotsPerLink.put(linkId, new MutableDouble(fac.getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity() ) );
			}
		}
		
		for(Id<Link> linkID : nrOfSlotsPerLink.keySet()){
//			Logger.getLogger(getClass()).info("initialize parking visualisation for link " + linkID);
			this.slotsOnLink.put(linkID, new ParkingSlotManager(network.getLinks().get(linkID), nrOfSlotsPerLink.get(linkID).intValue()));
		}
	}
	
	
	@Override
	public void reset(int iteration) {
		for(Id<Link> link : this.slotsOnLink.keySet()){
			this.slotsOnLink.get(link).setAllParkingTimesToZero();
		}
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if(this.slotsOnLink.containsKey(event.getLinkId())){
			this.vehiclesResponsibleManager.put(event.getVehicleId(), this.slotsOnLink.get(event.getLinkId()));
		}	
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		ParkingSlotManager manager = this.vehiclesResponsibleManager.remove(event.getVehicleId());
		if(manager != null){
			Tuple<Coord,Double> parkingTuple = manager.processParking(event.getTime(), event.getVehicleId());
			this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + event.getTime() + ";" +
					parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "free");
			this.parkedVehicles.put(event.getVehicleId(),manager.getLinkId());
		}
	}
	
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.parkedVehicles.containsKey(event.getVehicleId())){
			ParkingSlotManager manager = this.slotsOnLink.get(this.parkedVehicles.get(event.getVehicleId()));
			Tuple<Coord,Double> parkingTuple = manager.processUnParking(event.getTime(), event.getVehicleId());
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
		if (this.midnightParkers.containsKey(event.getVehicleId())){
			if(this.slotsOnLink.containsKey(event.getLinkId())){
				ParkingSlotManager manager = this.slotsOnLink.get(event.getLinkId());
				Tuple<Coord,Double> parkingTuple = manager.processUnParking(event.getTime(), event.getVehicleId());
				if(parkingTuple != null){
					this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + event.getTime() + ";" +
							parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "veh" + event.getVehicleId());
				}
			}
			this.midnightParkers.remove(event.getVehicleId());
		}
	}
	
	public void finishDay(){
		
		for(Id<Link> linkId : this.slotsOnLink.keySet()){
			ParkingSlotManager manager = this.slotsOnLink.get(linkId);
			Map<Id<Vehicle>,Tuple<Coord,Double>> occupiedSlots = manager.getOccupiedSlots();
			
			double endOfDay = 30*3600;
			for(Entry<Id<Vehicle>,Tuple<Coord,Double>> e : occupiedSlots.entrySet()){
				Tuple<Coord, Double> parkingTuple = e.getValue();
				this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + endOfDay + ";" +
						parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "veh" + e.getKey());
				
				// set back to 0
			}
			
			List<Tuple<Coord,Double>> freeSlots = manager.getFreeSlots();
			for(Tuple<Coord,Double> parkingTuple : freeSlots){
				this.parkings.add(manager.getLinkId() + ";" + parkingTuple.getSecond() + ";" + endOfDay + ";" +
						parkingTuple.getFirst().getX() + ";" + parkingTuple.getFirst().getY() + ";" + "free");
			}
		}
	}
	
	public void plotSlotOccupation(String filename){
		String head = "LinkId;from;To;X;Y;OccupiedByVehicle";
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write(head);
			for (String s : this.parkings){
				bw.newLine();
				bw.write(s);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.getLogger(getClass()).info("FINISHED WRITING PARKING SLOT VISUALISATION FILE TO: " +filename);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String path = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "ParkingSlots_it"+event.getIteration()+".csv");
		this.finishDay();
		this.plotSlotOccupation(path);
	}


	
}

class ParkingSlotManager{
	private List<Tuple<Coord,Double>> freeSlots = new ArrayList<Tuple<Coord,Double>>();
	private Map<Id<Vehicle>,Tuple<Coord,Double>> occupiedSlots = new HashMap<Id<Vehicle>,Tuple<Coord,Double>>();
	private Random r = MatsimRandom.getLocalInstance();
	private Id<Link> linkID;
	
	public ParkingSlotManager(Link link, int numberOfSlotsOnLink){
		for(Coord c : ParkingUtils.getEvenlyDistributedCoordsAlongLink(link, numberOfSlotsOnLink)){
			this.freeSlots.add(new Tuple<Coord,Double>(c, 0.0));
		}
		this.linkID = link.getId();
	}
	
	/**
	 * 
	 * @param timeOfParking
	 * @return tuple of the (now) occupied slot's coord and time point since it's been free
	 */
	public Tuple<Coord,Double> processParking(double timeOfParking, Id<Vehicle> vehID){
		if(this.freeSlots.size() == 0) throw new RuntimeException("all slots already occupied. cannot display another occupied slot");
		Tuple<Coord,Double> parkingTuple = this.freeSlots.remove(r.nextInt(this.freeSlots.size()));
		this.occupiedSlots.put(vehID, new Tuple<Coord,Double>(parkingTuple.getFirst(),timeOfParking) );
		return parkingTuple;
	}
	
	public void setAllParkingTimesToZero(){
		List<Tuple<Coord,Double>> newFreeSlots = new ArrayList<Tuple<Coord,Double>>();
		for (Tuple<Coord,Double> t : this.freeSlots){
			Coord c = t.getFirst();
			newFreeSlots.add(new Tuple<Coord, Double>(c,0.0));
		}
		this.freeSlots = newFreeSlots;
		for (Id<Vehicle> id : this.occupiedSlots.keySet()){
			Coord c = this.occupiedSlots.get(id).getFirst();
			this.occupiedSlots.put(id, new Tuple<Coord,Double>(c,0.0));
		}
	}
	/**
	 * 
	 * @param timeOfParking
	 * @return tuple of the (now) free slot's coord and time point since it's been occupied
	 */
	public Tuple<Coord, Double> processUnParking(double timeOfUnparking, Id<Vehicle> vehID){
		Tuple<Coord,Double> parkingTuple;
		if(this.occupiedSlots.size() == 0 || !(this.occupiedSlots.containsKey(vehID))){
//			throw new RuntimeException("or all slots already free or vehicle wasn't parked here.");
			if(freeSlots.size() == 0){
				return null;
			}
			parkingTuple= this.freeSlots.remove(r.nextInt(this.freeSlots.size()));
		}
		else{
			parkingTuple = this.occupiedSlots.remove(vehID);
		}
		this.freeSlots.add(new Tuple<Coord,Double>(parkingTuple.getFirst(),timeOfUnparking) );
		return parkingTuple;
	}
	
	public Id<Link> getLinkId(){
		return this.linkID;
	}
	
	public List<Tuple<Coord,Double>> getFreeSlots(){
		return this.freeSlots;
	}
	
	public Map<Id<Vehicle>,Tuple<Coord,Double>> getOccupiedSlots(){
		return this.occupiedSlots;
	}
}

