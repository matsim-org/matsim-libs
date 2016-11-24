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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ParkingSearchAndEgressTimeEvaluator implements PersonArrivalEventHandler, StartParkingSearchEventHandler, PersonEntersVehicleEventHandler{

	Map<Id<Person>,Double> searchTime = new HashMap<>();
	Map<Id<Vehicle>,Id<Person>> drivers = new HashMap<>();
	private Set<Id<Link>> monitoredLinks;
	private List<String> coordTimeStamps = new ArrayList<>();
	private double[] parkingCounts = new double[24];
	private double[] parkingTime = new double[24];
	private Network network;
	Random rnd = MatsimRandom.getLocalInstance();

	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		this.searchTime.clear();;
		this.drivers.clear();
	}

	/**
	 * 
	 */
	public ParkingSearchAndEgressTimeEvaluator(Set<Id<Link>> monitoredLinks, Network network) {
		this.monitoredLinks = monitoredLinks;
		this.network = network;
		Locale.setDefault(new Locale("en", "US"));

	}
	
	public void writeStats(String filename){
	BufferedWriter bw = IOUtils.getBufferedWriter(filename);
	DecimalFormat df = new DecimalFormat("##.##");	
		try {
			bw.write("hour;parkingCounts;averageSearchAndEgressWalkTime");
			for (int i = 0; i<this.parkingCounts.length;i++){
				bw.newLine();
				bw.write(i+";"+parkingCounts[i]+";"+df.format(parkingTime[i]/parkingCounts[i]));
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	public void writeCoordTimeStamps(String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try {
				bw.write("time;coordX;coordY;Xwgs;Ywgs;searchTime");
				for (String s: this.coordTimeStamps){
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
	

	
	


	
	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.events.StartParkingSearchEventHandler#handleEvent(playground.jbischoff.parking.events.StartParkingSearchEvent)
	 */
	@Override
	public void handleEvent(StartParkingSearchEvent event) {
		if (this.monitoredLinks.contains(event.getLinkId())){
		Id<Person> pid = this.drivers.get(event.getVehicleId());
		if (pid!=null){
		this.searchTime.put(pid, event.getTime());	
		}
	}}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		//Assumes: Agent = driver, or at least the person that initiates a ride (i.e. a taxi passenger that lets his taxi search for parking or so...)
		this.drivers.put(event.getVehicleId(), event.getPersonId());
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (this.searchTime.containsKey(event.getPersonId())){
			if (event.getLegMode().equals(TransportMode.egress_walk)){
			double parkingTime = event.getTime() - searchTime.remove(event.getPersonId());
			int hour = (int) (event.getTime() / 3600);
			if (hour<24){
				this.parkingCounts[hour]++;
				this.parkingTime[hour]+=parkingTime;
				CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.GK4, TransformationFactory.WGS84);
				Coord coord = ParkingUtils.getRandomPointAlongLink(rnd, this.network.getLinks().get(event.getLinkId()));
				Coord t = ct.transform(coord);
				String stamp = event.getTime()+";"+coord.getX()+";"+coord.getY()+";"+t.getX()+";"+t.getY()+";"+parkingTime;
				this.coordTimeStamps.add(stamp);
			}
		}
			}
	}

}
