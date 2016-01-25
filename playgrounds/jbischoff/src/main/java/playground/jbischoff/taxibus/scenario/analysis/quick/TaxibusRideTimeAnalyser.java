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
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.postgresql.jdbc2.TimestampUtils;

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
	
	private double[] hourlyDepartures = new double[24];
	private double[] hourlyTourDepartures = new double[24];
	

	
	
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
		int hour = getHour(event.getTime());
		
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
	
	private int getHour(double time){
		int hour = (int) Math.floor(time/(3600));
		if (hour>23){
			hour = hour%24;
		}
		return hour;
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

		return vid + "\t"+ Time.writeTime(firstPickup) + "\t" + Time.writeTime(lastPickup) + "\t" + Time.writeTime(occupancy)
				+ "\t" + Time.writeTime(pickUpDuration) + "\t" + Time.writeTime(dropoffTime);
	}




	@Override
	public int compareTo(TaxibusTour arg0) {
		return firstPickup.compareTo(arg0.firstPickup);
	}
	
}
