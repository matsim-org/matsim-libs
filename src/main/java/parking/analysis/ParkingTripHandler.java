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
  
package parking.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import parking.ParkingZone;
import parking.ZonalLinkParkingInfo;

public class ParkingTripHandler implements LinkEnterEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, ActivityEndEventHandler, ActivityStartEventHandler, TeleportationArrivalEventHandler, PersonEntersVehicleEventHandler {

	
	private Map<Id<Person>,ParkTrip> parkTrips = new HashMap<>();
	private Map<Id<Person>,ActivityEndEvent> previousActivity = new HashMap<>();
	private Map<Id<Vehicle>,Id<Person>> vehicleUsers = new HashMap<>();
	private List<ParkTrip> completedTrips = new ArrayList<>(); 
	private final Network network;
	private String s = ";";
	private ZonalLinkParkingInfo info;
	private Population population;
	
	
	@Inject
	public ParkingTripHandler(Network network, ZonalLinkParkingInfo info, Population population) {
		this.network = network;
		this.info = info;
		this.population = population;
	}
	
	
	@Override
	public void reset(int iteration) {
		parkTrips.clear();
		previousActivity.clear();
		completedTrips.clear();
		vehicleUsers.clear();
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!population.getPersons().containsKey(event.getPersonId())) return;
		if (event.getActType().endsWith("interaction")) return;
		if (event.getActType().endsWith("parkingSearch")) return;

		if (this.parkTrips.containsKey(event.getPersonId())){
				ParkTrip trip = parkTrips.remove(event.getPersonId());
				trip.followingActivity = event.getActType();
				trip.followingActivityLink = event.getLinkId();
				completedTrips.add(trip);
			
		}
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().endsWith("interaction")) return;
		if (!population.getPersons().containsKey(event.getPersonId())) return;
		
		if (!event.getActType().endsWith("parkingSearch")) {
			previousActivity.put(event.getPersonId(), event);
		} else  {
			this.parkTrips.get(event.getPersonId()).parkMode = true;
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!population.getPersons().containsKey(event.getPersonId())) return;
		if (event.getLegMode().equals(TransportMode.access_walk)) {
			//could also be a pt trip, we don't know this yet!
			ParkTrip trip = new ParkTrip(event.getPersonId());
			trip.accessWalkDepartureTime = event.getTime();
			parkTrips.put(event.getPersonId(), trip);
		} else if (event.getLegMode().equals(TransportMode.car)) {
			ParkTrip trip = parkTrips.get(event.getPersonId());
			if (trip == null) {
				trip = new ParkTrip(event.getPersonId());
				trip.accessWalkDepartureTime = event.getTime();
				trip.accessWalkArrivalTime = event.getTime();
				parkTrips.put(event.getPersonId(), trip);
			}
			ActivityEndEvent aee = previousActivity.get(event.getPersonId());
			trip.previousActivityLink = aee.getLinkId();
			trip.previousActivity = aee.getActType();
			
			if (trip.parkMode) {
				trip.parkLegDepartureTime = event.getTime();
			} else {
				trip.carLegDepartureTime = event.getTime();
				
			}
		} else if (event.getLegMode().equals(TransportMode.egress_walk)) {
			if (parkTrips.containsKey(event.getPersonId())) {
				ParkTrip trip = parkTrips.get(event.getPersonId());
				trip.egressWalkDepartureTime = event.getTime();
			}} 
		else if (event.getLegMode().equals(TransportMode.pt)) {
				//any other mode, such as pt. Time to remove what we have already stored.
				parkTrips.remove(event.getPersonId());
			}
		
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!population.getPersons().containsKey(event.getPersonId())) return;
		if (event.getLegMode().equals(TransportMode.access_walk)) {
			ParkTrip trip = parkTrips.get(event.getPersonId());
			trip.accessWalkArrivalTime = event.getTime();
		} else if (event.getLegMode().equals(TransportMode.car)) {
			ParkTrip trip = parkTrips.get(event.getPersonId());
			if (trip.parkMode) {
				trip.parkLegArrivalTime = event.getTime();
			} else {
				trip.carLegArrivalTime = event.getTime();
			}
		} else if (event.getLegMode().equals(TransportMode.egress_walk)) {
			if (parkTrips.containsKey(event.getPersonId())) {
				ParkTrip trip = parkTrips.get(event.getPersonId());
				trip.egressWalkArrivalTime = event.getTime();
			}

		}

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (vehicleUsers.containsKey(event.getVehicleId())) {
			double linkLength = network.getLinks().get(event.getLinkId()).getLength();
			ParkTrip trip = this.parkTrips.get(vehicleUsers.get(event.getVehicleId()));
			if (trip.parkMode) {
				trip.parkLegDistance_m+=linkLength;
			} else {
				trip.carLegDistance_m+=linkLength;
			}
		}
		
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		if (parkTrips.containsKey(event.getPersonId())) {
			ParkTrip trip = parkTrips.get(event.getPersonId());
			if (trip.parkMode) {
				trip.egressWalkDistance_m = event.getDistance();
			} else {
				trip.accessWalkDistance_m = event.getDistance();
				
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(parkTrips.containsKey(event.getPersonId())) {
			vehicleUsers.put(event.getVehicleId(), event.getPersonId());
		}
	}
	
	public void writeParkingTrips(String file){
		BufferedWriter bw = IOUtils.getBufferedWriter(file);
		try {
			bw.write("personId"+s+"previousActivity"+s+"fromLink"+s+"fromX"+s+"fromY"+s+"nextActivity"+s+"toX"+s+"toY"+s+"toZone"+s+"departureTime"+
					s+"arrivalTime"+s+"travelTime"+s+"accessWalkTime"+s+"accessWalkDistance"+s+"carTravelTime"+s+"carTravelDistance"+s+"parkSearchTime"+s+"parkSearchDistance"+s+"egressWalkTime"+
					s+"egressWalkDistance");
			for (ParkTrip trip : completedTrips) {
				bw.newLine();
//				System.out.println(trip.toString());
				Coord fromCoord = network.getLinks().get(trip.previousActivityLink).getCoord(); 
				Coord toCoord = network.getLinks().get(trip.followingActivityLink).getCoord(); 
				int accessWalkTime = (int) (trip.accessWalkArrivalTime- trip.accessWalkDepartureTime); 
				int egressWalkTime = (int) (trip.egressWalkArrivalTime- trip.egressWalkDepartureTime);
				int travelTime = (int) (trip.egressWalkArrivalTime-trip.accessWalkDepartureTime);
				int carTravelTime = (int) (trip.carLegArrivalTime-trip.carLegDepartureTime);
				int searchTime = (int) (trip.parkLegArrivalTime-trip.parkLegDepartureTime);
				ParkingZone z = info.getParkingZone(network.getLinks().get(trip.followingActivityLink));
				String toZone = "null";
				if (z!=null) {
					toZone = z.getId().toString();
				}
				
				bw.write(trip.getPersonId().toString()+s+trip.previousActivity+s+trip.previousActivityLink.toString()+s+fromCoord.getX()+s+fromCoord.getY()+s+trip.followingActivityLink.toString()+s+toCoord.getX()+s+toCoord.getY()+s+toZone
				+s+trip.accessWalkDepartureTime+s+trip.egressWalkArrivalTime+s+travelTime+s+accessWalkTime+s+trip.accessWalkDistance_m+carTravelTime+s+trip.carLegDistance_m+s+searchTime+s+trip.parkLegDistance_m+s+egressWalkTime+s+trip.egressWalkDistance_m);
			}
		
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
}

	class ParkTrip  {
		
		ParkTrip(Id<Person> personId){
			this.personId = personId;
		}
		boolean parkMode = false;
		
		private Id<Person> personId;
		
		String previousActivity;
		Id<Link> previousActivityLink;
		
		String followingActivity;
		Id<Link> followingActivityLink;
		
		double accessWalkDepartureTime = 0;
		double accessWalkArrivalTime = 0;
		double accessWalkDistance_m = 0;
		
		double carLegDepartureTime;
		double carLegArrivalTime;
		double carLegDistance_m;
		
		double parkLegDepartureTime;
		double parkLegArrivalTime;
		double parkLegDistance_m;
		
		double egressWalkDepartureTime = 0;
		double egressWalkArrivalTime= 0;
		double egressWalkDistance_m= 0;
		
		public Id<Person> getPersonId() {
			return personId;
		}

		@Override
		public String toString() {
			return "personId=" + personId + "; previousActivity=" + previousActivity + "; previousActivityLink="
					+ previousActivityLink + "; followingActivity=" + followingActivity + "; followingActivityLink="
					+ followingActivityLink + "; accessWalkDepartureTime=" + accessWalkDepartureTime
					+ "; accessWalkArrivalTime=" + accessWalkArrivalTime + "; accessWalkDistance_m="
					+ accessWalkDistance_m + "; carLegDepartureTime=" + carLegDepartureTime + "; carLegArrivalTime="
					+ carLegArrivalTime + "; carLegDistance_m=" + carLegDistance_m + "; parkLegDepartureTime="
					+ parkLegDepartureTime + "; parkLegArrivalTime=" + parkLegArrivalTime + "; parkLegDistance_m="
					+ parkLegDistance_m + "; egressWalkDepartureTime=" + egressWalkDepartureTime
					+ "; egressWalkArrivalTime=" + egressWalkArrivalTime + "; egressWalkDistance_m="
					+ egressWalkDistance_m;
		}
		
		
		
	}
