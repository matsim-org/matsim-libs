/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.ana;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collect some data for iatbr paper. Currently:
 * <li> Number of trips served by train
 * <li> Number of minibuses which serve at least one trip
 * <li> Kilometer traveled by all minibuses
 * <li> Passenger kilometer served by minibuses
 * <li> Number of passengers using train and minibus service
 * 
 * @author aneumann
 *
 */
final class IatbrAna implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(IatbrAna.class);
	
	private final String pIdentifier;
	private final String trainIdentifier;
	
	private int numberOfTripsServedByTrain = 0;
	private int numberOfTripsServedByMinibus = 0;
	private Set<Id<Vehicle>> minibusesWithAtLeastOneTrip = new TreeSet<>();
	private double kmTravelledByMinibuses = 0.0;
	private double passengerKmMinibus = 0.0;
	private int numberOfPassengersUsingTrainAndMinibus;

	private HashMap<Id<Vehicle>, Integer> vehId2NumberOfPassengers = new HashMap<>();
	private HashMap<Id<Person>, String> agentId2ModeAlreadyUsed = new HashMap<>();
	
	private IatbrAna(String pIdentifier, String trainIdentifier){
		this.pIdentifier = pIdentifier;
		this.trainIdentifier = trainIdentifier;
	}
	
	public static void main(String[] args) {
		
		ArrayList<String> scenarioIds = new ArrayList<>();
		scenarioIds.add("1min");
		scenarioIds.add("25min");
		scenarioIds.add("5min");
		scenarioIds.add("10min");
		scenarioIds.add("15min");
		scenarioIds.add("20min");
		scenarioIds.add("30min");
		scenarioIds.add("60min");
		
		IatbrAna ana = new IatbrAna("para_", "tr_");
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(ana);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(manager);
		
		String resultsFile = "e:/_runs-svn/run1650/key_figures.txt";
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(resultsFile)));
			writer.write("# scenarioId\tnumberOfTripsTrain\tnumberOfTripsMinibus\tnumberOfMinibusesWithAtLeastOneTrip\tkmTravlledByAllMinibuses\tpassengerKmMinibus\tnumberOfPassengersUsingTrainAndMinibus"); writer.newLine();
			
			for (String scenarioId : scenarioIds) {
				ana.reset(0);
				String eventsFile = "e:/_runs-svn/run1650/all2all_" + scenarioId + "/ITERS/it.10000/all2all_" + scenarioId + ".10000.events.xml.gz";
				reader.readFile(eventsFile);
				ana.writeToFile(writer, scenarioId);			
			}
		
			writer.close();	
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void writeToFile(BufferedWriter writer, String scenarioId) throws IOException {
		writer.write(scenarioId + "\t" + this.numberOfTripsServedByTrain + "\t" + this.numberOfTripsServedByMinibus + "\t" + this.minibusesWithAtLeastOneTrip.size() + "\t" + this.kmTravelledByMinibuses + "\t" + this.passengerKmMinibus + "\t" + this.numberOfPassengersUsingTrainAndMinibus); writer.newLine();
		writer.flush();
	}

	@Override
	public void reset(int iteration) {
		this.numberOfTripsServedByTrain = 0;
		this.numberOfTripsServedByMinibus = 0;
		this.minibusesWithAtLeastOneTrip = new TreeSet<>();
		this.kmTravelledByMinibuses = 0.0;
		this.passengerKmMinibus = 0.0;
		this.vehId2NumberOfPassengers = new HashMap<>();
		this.numberOfPassengersUsingTrainAndMinibus = 0;
		this.agentId2ModeAlreadyUsed = new HashMap<>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().contains(this.trainIdentifier)){
			if(!event.getPersonId().toString().contains(this.trainIdentifier)){
				this.numberOfTripsServedByTrain++;
				
				if (!this.agentId2ModeAlreadyUsed.containsKey(event.getPersonId())) {
					this.agentId2ModeAlreadyUsed.put(event.getPersonId(), this.trainIdentifier);
				} else {
					if (this.agentId2ModeAlreadyUsed.get(event.getPersonId()).equalsIgnoreCase(this.pIdentifier)) {
						this.numberOfPassengersUsingTrainAndMinibus++;
					} else {
						this.agentId2ModeAlreadyUsed.put(event.getPersonId(), this.trainIdentifier);
					}
				}
				
			}
		}
		
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if (this.vehId2NumberOfPassengers.get(event.getVehicleId()) == null) {
				this.vehId2NumberOfPassengers.put(event.getVehicleId(), 0);
			}
			
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.minibusesWithAtLeastOneTrip.add(event.getVehicleId());
				this.numberOfTripsServedByMinibus++;

				this.vehId2NumberOfPassengers.put(event.getVehicleId(), this.vehId2NumberOfPassengers.get(event.getVehicleId()) + 1);
				
				if (!this.agentId2ModeAlreadyUsed.containsKey(event.getPersonId())) {
					this.agentId2ModeAlreadyUsed.put(event.getPersonId(), this.pIdentifier);
				} else {
					if (this.agentId2ModeAlreadyUsed.get(event.getPersonId()).equalsIgnoreCase(this.trainIdentifier)) {
						this.numberOfPassengersUsingTrainAndMinibus++;
					} else {
						this.agentId2ModeAlreadyUsed.put(event.getPersonId(), this.pIdentifier);
					}
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.vehId2NumberOfPassengers.put(event.getVehicleId(), this.vehId2NumberOfPassengers.get(event.getVehicleId()) - 1);
			}
		}
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			double linkLength;
			if (event.getLinkId().toString().equalsIgnoreCase("A") || event.getLinkId().toString().equalsIgnoreCase("B") || event.getLinkId().toString().equalsIgnoreCase("C") || event.getLinkId().toString().equalsIgnoreCase("D")) {
				linkLength = 0.1;
			} else {
				linkLength = 1.2;
			}
			this.kmTravelledByMinibuses += linkLength;
			this.passengerKmMinibus += this.vehId2NumberOfPassengers.get(event.getVehicleId()) * linkLength;
		}		
	}
	
}
