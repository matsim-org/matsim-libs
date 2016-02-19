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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *	Analysis tool for taxibus trips. 
 *	Should be working for shared trips, as long as all pick-ups are handled before drop offs, i.e. in most cases where hubing in spoking is involved.
 *	The tool will produce less meaningful results if pickups and drop offs are mixed.
 */
public class TaxibusTourAnalyser implements ActivityEndEventHandler, ActivityStartEventHandler, LinkEnterEventHandler {
	
	private HashSet<Id<Vehicle>> vehicles = new HashSet<>();
	private Set<TaxibusTour> tours = new TreeSet<>();
	
	private Map<Id<Vehicle>,Integer> currentRidePax = new HashMap<>();
	private Map<Id<Vehicle>,Integer> paxDroppedOff = new HashMap<>();

	private Map<Id<Vehicle>,Double> lastPickupOnTour = new HashMap<>();
	private Map<Id<Vehicle>,Double> firstPickupOnTour = new HashMap<>();
	
	private Map<Id<Vehicle>,Double> firstDropoffOnTour = new HashMap<>();
	
	
	private Map<Id<Vehicle>,Double> overallDistanceOnTour = new HashMap<>();
	private Map<Id<Vehicle>,Double> pickUpDistanceOnTour = new HashMap<>();

	private final Network network;
	
	private double[] hourlyDepartures = new double[24];
	private double[] hourlyTourDepartures = new double[24];
	
	public TaxibusTourAnalyser(Network network) {
		this.network = network;
	}
	
	
	@Override
	public void reset(int iteration) {

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
		else if (event.getActType().startsWith("Stay")){
			handleStay(event);
		}
		
		

	}

	private void handleStay(ActivityEndEvent event) {
		//Stay Task ends - begin of dispatch == begin of tour
		Id<Vehicle> vid = p2vid(event.getPersonId());
		this.overallDistanceOnTour.put(vid, 0.0);
		
		
	}

	private void handleDropoff(ActivityEndEvent event) {
		Id<Vehicle> vid = p2vid(event.getPersonId());
		if (lastPickupOnTour.containsKey(vid)){
		int dropoffNo= paxDroppedOff.get(vid);	
		if (dropoffNo ==0){
			firstDropoffOnTour.put(vid, event.getTime());
			dropoffNo++;
			paxDroppedOff.put(vid, dropoffNo);
		}	
		else if (dropoffNo < (currentRidePax.get(vid)-1)){
			dropoffNo++;
			paxDroppedOff.put(vid, dropoffNo);
		}
		else if (dropoffNo == (currentRidePax.get(vid)-1)){
		
			
			double lastPickup = lastPickupOnTour.remove(vid);
			double firstPickup = firstPickupOnTour.remove(vid);
			int occupancy = currentRidePax.remove(vid);
			double pickUpDuration = lastPickup-firstPickup;
			double firstDropoff = firstDropoffOnTour.get(vid);
			double lastDropoff = event.getTime();
			double dropOffDuration = lastDropoff-firstDropoff;
			double overallDistance = overallDistanceOnTour.remove(vid);
			double pickupDistance = pickUpDistanceOnTour.remove(vid);
			
			TaxibusTour tour = new TaxibusTour(occupancy, firstPickup, lastPickup, pickUpDuration, firstDropoff, lastDropoff, dropOffDuration, pickupDistance, overallDistance, vid);
			this.tours.add(tour);
			
		}
			
		
		
		}
	}

	private void handlePickup(ActivityEndEvent event) {
		int hour = JbUtils.getHour(event.getTime());
		
		Id<Vehicle> vid = p2vid(event.getPersonId());
		int pax = 1;
		if (currentRidePax.containsKey(vid)){
			pax += currentRidePax.get(vid); 
		}
		currentRidePax.put(vid, pax);
		lastPickupOnTour.put(vid, event.getTime());
		hourlyDepartures[hour]++;
		if (!firstPickupOnTour.containsKey(vid)){
			firstPickupOnTour.put(vid, event.getTime());
			hourlyTourDepartures[hour]++;
			pickUpDistanceOnTour.put(vid, overallDistanceOnTour.get(vid));
			this.paxDroppedOff.put(vid, 0);
		}
	}

	private void handleBeforeScheduleEvent(ActivityEndEvent event) {
		vehicles.add(p2vid(event.getPersonId()));
	}

	public void writeOutput(String folder){
		writeTours(folder+"/taxibus_tours.txt");
		writeHourlyStats(folder+"/taxibus_hourlyStats.txt");
	}

	private void writeHourlyStats(String hourlyStats) {
		
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat( "##,##0.00" );
		DecimalFormat lf = new DecimalFormat( "##,##0" );
		
		BufferedWriter writer = IOUtils.getBufferedWriter(hourlyStats);
		try {
			writer.append("hour\tDepartures\tRides\tOccupancy");
			for (int i = 0; i<24; i++){
				writer.newLine();
				double occ = 0.0;
				if (hourlyTourDepartures[i]>0) occ = hourlyDepartures[i]/hourlyTourDepartures[i];
				
				String result = df.format(occ);
				writer.append(i+"\t"+lf.format(hourlyTourDepartures[i])+"\t"+lf.format(hourlyDepartures[i])+"\t"+result);
								
			}
			writer.newLine();
			double allTourDepartures = new Sum().evaluate(hourlyTourDepartures);
			double allRides = new Sum().evaluate(hourlyDepartures);
			writer.append("overall\t"+lf.format(allTourDepartures)+"\t"+lf.format(allRides)+"\t"+df.format(allRides/allTourDepartures));
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void writeTours(String toursFile) {
		BufferedWriter writer = IOUtils.getBufferedWriter(toursFile);
		try {
			writer.append("Vehicle\tFirstPickUp\tlastPickup\tPickUpDuration\tfirstDropOff\tlastDropoff\tDropoffDuration\tTourDistance\tPickupDistance\tOccupancy");
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
	
	private Id<Vehicle> v2vid (Id<org.matsim.vehicles.Vehicle> vehicleId){
		Id<Vehicle> vid = Id.create(vehicleId.toString(), Vehicle.class);
		return vid;
	}
	


	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Vehicle> vid = v2vid(event.getVehicleId());
		if (overallDistanceOnTour.containsKey(vid)){
			double distance = overallDistanceOnTour.get(vid);
			distance += network.getLinks().get(event.getLinkId()).getLength();
			overallDistanceOnTour.put(vid, distance);
		}
		
	}
}
class TaxibusTour implements Comparable<TaxibusTour>{
	int occupancy;
	DecimalFormat df = new DecimalFormat( "##,##0.00" );

	Double firstPickup;
	double lastPickup;
	double pickUpDuration;
	
	double firstDropoff;
	double lastDropoff;
	double dropOffDuration;
	
	double pickupDistance;
	double overallDistance;
	
	
	Id<Vehicle> vid;
	
	
	TaxibusTour(int occupancy, Double firstPickup, double lastPickup, double pickUpDuration, double firstDropoff,
			double lastDropoff, double dropOffDuration, double pickupDistance, double overallDistance,
			Id<Vehicle> vid) {
		this.occupancy = occupancy;
		this.firstPickup = firstPickup;
		this.lastPickup = lastPickup;
		this.pickUpDuration = pickUpDuration;
		this.firstDropoff = firstDropoff;
		this.lastDropoff = lastDropoff;
		this.dropOffDuration = dropOffDuration;
		this.pickupDistance = pickupDistance;
		this.overallDistance = overallDistance;
		this.vid = vid;
	}




	@Override
	public String toString() {

		return vid + "\t"+ Time.writeTime(firstPickup) + "\t" + Time.writeTime(lastPickup) + "\t" + Time.writeTime(pickUpDuration) +"\t"+
				Time.writeTime(firstDropoff) + "\t" + Time.writeTime(lastDropoff) + "\t" + Time.writeTime(dropOffDuration) 
				+ "\t" + df.format(overallDistance/1000)+ "\t" + df.format(pickupDistance/1000) + "\t" + occupancy;
				
	}




	@Override
	public int compareTo(TaxibusTour arg0) {
		return firstPickup.compareTo(arg0.firstPickup);
	}
	
}
