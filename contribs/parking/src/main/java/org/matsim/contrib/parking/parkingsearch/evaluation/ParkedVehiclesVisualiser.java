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

/**
 * 
 */
package org.matsim.contrib.parking.parkingsearch.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ParkedVehiclesVisualiser implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {

		Network network;
	
	/**
	 * 
	 */
	@Inject
	public ParkedVehiclesVisualiser(Network network) {
		this.network = network;
	}
	
	Map<Id<Vehicle>,Tuple<Id<Link>,MutableDouble>> vehicleParkingPosition = new HashMap<>();
	Map<Id<Vehicle>,Tuple<Double,Double>> midnightParkers = new HashMap<>();
	Map<Id<Vehicle>,Id<Link>> lastLink = new HashMap<>();
	Random r = MatsimRandom.getLocalInstance();
	List<String> parkings = new ArrayList<>();
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		
	}

	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		vehicleParkingPosition.put(event.getVehicleId(), new Tuple<Id<Link>,MutableDouble>(lastLink.get(event.getVehicleId()),new MutableDouble(event.getTime())));
	}


	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Tuple<Id<Link>,MutableDouble> lastLoc = vehicleParkingPosition.remove(event.getVehicleId());
		if (lastLoc!=null){
			Id<Link> linkId = lastLoc.getFirst();
			double parkTime = lastLoc.getSecond().doubleValue();
			double unparkTime = event.getTime();
			Link l = network.getLinks().get(linkId);
			Coord coord = ParkingUtils.getRandomPointAlongLink(r, l);
			parkings.add(event.getVehicleId()+";"+parkTime+";"+unparkTime+";"+coord.getX()+";"+coord.getY());
		} else {
			midnightParkers.put(event.getVehicleId(), new Tuple<Double,Double>(0.0,event.getTime()));
		}
	}


	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.lastLink.put(event.getVehicleId(), event.getLinkId());
		
		
	}


	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler#handleEvent(org.matsim.api.core.v01.events.VehicleEntersTrafficEvent)
	 */
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (this.midnightParkers.containsKey(event.getVehicleId())){
			Link l = network.getLinks().get(event.getLinkId());
			Coord coord = ParkingUtils.getRandomPointAlongLink(r, l);
			Tuple<Double, Double> time = midnightParkers.remove(event.getVehicleId());
			parkings.add(event.getVehicleId()+";"+time.getFirst()+";"+time.getSecond()+";"+event.getLinkId()+";"+coord.getX()+";"+coord.getY());
		}
	}
	
	public void finishDay(){
		for (Entry<Id<Vehicle>, Tuple<Id<Link>, MutableDouble>> e :vehicleParkingPosition.entrySet()){
			Tuple<Id<Link>,MutableDouble> lastLoc = e.getValue();
			Id<Vehicle> vehicleId = e.getKey(); 
			Id<Link> linkId = lastLoc.getFirst();
			double parkTime = lastLoc.getSecond().doubleValue();
			double unparkTime = 30*3600;
			Link l = network.getLinks().get(linkId);
			Coord coord = ParkingUtils.getRandomPointAlongLink(r, l);
			parkings.add(vehicleId+";"+parkTime+";"+unparkTime+";"+linkId+";"+coord.getX()+";"+coord.getY());
		}
	}
	
	public void plotCarPositions(String filename){
		String head = "Vehicle;parkTime;unparkTime;LinkId;X;Y";
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
