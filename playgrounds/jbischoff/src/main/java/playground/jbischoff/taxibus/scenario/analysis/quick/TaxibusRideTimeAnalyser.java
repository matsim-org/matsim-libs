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

package playground.jbischoff.taxibus.scenario.analysis.quick;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author  jbischoff
 *
 */
public class TaxibusRideTimeAnalyser implements ActivityEndEventHandler, ActivityStartEventHandler {
	
	private HashSet<Id<Vehicle>> vehicles = new HashSet<>();
	private Set<TaxibusTour> tours = new TreeSet<>();
	
	private Map<Id<Vehicle>,Integer> currentRidePax = new HashMap<>();
	private Map<Id<Vehicle>,Double> lastPickupOnTour = new HashMap<>();
	private Map<Id<Vehicle>,Double> firstPickupOnTour = new HashMap<>();
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().startsWith("Before schedule")){
			handleBeforeScheduleEvent(event);
		} else if (event.getActType().startsWith("PassengerPickup")){
			handlePickup(event);
		} else if (event.getActType().startsWith("PassengerDropoff")){
			handleDropoff(event);
		}
		
		

	}

	private void handleDropoff(ActivityEndEvent event) {
		Id<Vehicle> vid = p2vid(event.getPersonId());
		if (lastPickupOnTour.containsKey(vid)){
		double lastPickup = lastPickupOnTour.remove(vid);
		double firstPickup = firstPickupOnTour.remove(vid);
		int occupancy = currentRidePax.remove(vid);
		double pickUpDuration = lastPickup-firstPickup;
		
		TaxibusTour tour = new TaxibusTour(firstPickup, lastPickup, occupancy, pickUpDuration, event.getTime(), vid) ;
		
		this.tours.add(tour);
		}
	}

	private void handlePickup(ActivityEndEvent event) {
		
		Id<Vehicle> vid = p2vid(event.getPersonId());
		int pax = 1;
		if (currentRidePax.containsKey(vid)){
			pax += currentRidePax.get(vid); 
		}
		currentRidePax.put(vid, pax);
		lastPickupOnTour.put(vid, event.getTime());
		
		if (!firstPickupOnTour.containsKey(vid)){
			firstPickupOnTour.put(vid, event.getTime());
		}
	}

	private void handleBeforeScheduleEvent(ActivityEndEvent event) {
		vehicles.add(p2vid(event.getPersonId()));
	}

	public void writeOutput(String folder){
		BufferedWriter writer = IOUtils.getAppendingBufferedWriter(folder+"/taxibustours.txt");
		try {
			writer.append("Vehicle\tFirstPickUp\tlastPickup\tOccupancy\tPickUpDuration\tDropOffTime");
			for (TaxibusTour tour : this.tours){
				writer.newLine();
				writer.append(tour.toString());
								
			}
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private Id<Vehicle> p2vid (Id<Person> pid){
		Id<Vehicle> vid = Id.create(pid.toString(), Vehicle.class);
		return vid;
	}
}
class TaxibusTour implements Comparable<TaxibusTour>{
		
	Double firstPickup;
	double lastPickup;
	int occupancy;
	double pickUpDuration;
	double dropoffTime;
	Id<Vehicle> vid;
	
	
	
	TaxibusTour(Double firstPickup, double lastPickup, int occupancy, double pickUpDuration, double dropoffTime,
			Id<Vehicle> vid) {
		super();
		this.firstPickup = firstPickup;
		this.lastPickup = lastPickup;
		this.occupancy = occupancy;
		this.pickUpDuration = pickUpDuration;
		this.dropoffTime = dropoffTime;
		this.vid = vid;
	}

	

	@Override
	public String toString() {
		return vid + "\t"+ firstPickup + "\t" + lastPickup + "\t" + occupancy
				+ "\t" + pickUpDuration + "\t" + dropoffTime;
	}



	@Override
	public int compareTo(TaxibusTour arg0) {
		return firstPickup.compareTo(arg0.firstPickup);
	}
	
}
