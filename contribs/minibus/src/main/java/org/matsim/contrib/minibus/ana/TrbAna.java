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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collect some data for iatbr paper. Currently:
 * <li> Number of trips served by pt
 * <li> Number of minibuses which serve at least one trip
 * <li> Kilometer traveled by all minibuses
 * <li> Passenger kilometer served by minibuses
 * <li> Number of passengers using pt and minibus service
 * <li> Number of boarding passengers in given service area pt
 * <li> Number of alighting passengers in given service area pt
 * <li> Number of boarding passengers in given service area minibus
 * <li> Number of alighting passengers in given service area minibus
 * 
 * @author aneumann
 *
 */
final class TrbAna implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, VehicleArrivesAtFacilityEventHandler{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(TrbAna.class);
	
	private final String pIdentifier;
	private final String ptIdentifier;
	private final Network network;
	private final TransitSchedule transitSchedule;
	
	private final double minX;
	private final double maxX;
	private final double minY;
	private final double maxY;
	
	private int numberOfTripsServedByPt = 0;
	private Set<Id<Vehicle>> minibusesWithAtLeastOneTrip = new TreeSet<>();
	private double kmTravelledByMinibuses = 0.0;
	private double passengerKmMinibus = 0.0;
	private int numberOfPassengersUsingPtAndMinibus;

	private HashMap<Id<Vehicle>, Integer> vehId2NumberOfPassengers = new HashMap<>();
	private HashMap<Id<Person>, String> agentId2ModeAlreadyUsed = new HashMap<>();
	private HashMap<Id<Vehicle>, Id<TransitStopFacility>> vehId2StopIdMap = new HashMap<>();

	private int boardingInServiceAreaPt = 0;
	private int alightingInServiceAreaPt = 0;
	private int boardingInServiceAreaP = 0;
	private int alightingInServiceAreaP = 0;


	
	private TrbAna(String pIdentifier, String ptIdentifier, Network network, TransitSchedule transitSchedule, Config config){
		this.pIdentifier = pIdentifier;
		this.ptIdentifier = ptIdentifier;
		this.network = network;
		this.transitSchedule = transitSchedule;
		this.minX = Double.parseDouble(config.getParam(PConfigGroup.GROUP_NAME, "minX"));
		this.maxX = Double.parseDouble(config.getParam(PConfigGroup.GROUP_NAME, "maxX"));
		this.minY = Double.parseDouble(config.getParam(PConfigGroup.GROUP_NAME, "minY"));
		this.maxY = Double.parseDouble(config.getParam(PConfigGroup.GROUP_NAME, "maxY"));
	}
	
	public static void main(String[] args) {

		Config c = ConfigUtils.loadConfig("F:/p_runs/txl/config.xml");
	
		String transitSchedule = "F:/p_runs/txl/txl_s_transitSchedule.xml.gz";
		String scenarioId = "run12";
		String oldId = "s1";
		String iteration = "180";
		String eventsFile = "F:/p_runs/txl/" + scenarioId + "/it." + iteration + "/txl_" + oldId + "." + iteration + ".events.xml.gz";
		String resultsFile = "F:/p_runs/txl/" + scenarioId + "/" + scenarioId + "_it." + iteration + "_key_figures.txt";
		
		Scenario sc = ScenarioUtils.createScenario(c);
		new MatsimNetworkReader(sc.getNetwork()).readFile("F:/p_runs/txl/network.final.xml.gz");
		new TransitScheduleReader(sc).readFile(transitSchedule);
		
		TrbAna ana = new TrbAna("para_", "tr_", sc.getNetwork(), sc.getTransitSchedule(), c);
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(ana);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(manager);
		
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(resultsFile)));
			
			writer.write("# Minibus service area - minX: " + Double.parseDouble(c.getParam(PConfigGroup.GROUP_NAME, "minX")) 
					+ ", maxX: " + Double.parseDouble(c.getParam(PConfigGroup.GROUP_NAME, "maxX")) 
					+ ", minY: " + Double.parseDouble(c.getParam(PConfigGroup.GROUP_NAME, "minY")) 
					+ ", maxY: " + Double.parseDouble(c.getParam(PConfigGroup.GROUP_NAME, "maxY"))); writer.newLine();
			writer.write("# scenarioId\tnumberOfTripsPt\tnumberOfMinibusesWithAtLeastOneTrip\tkmTravelledByAllMinibuses\tpassengerKmMinibus\tnumberOfPassengersUsingPtAndMinibus\tboardingInServiceAreaP\talightingInServiceAreaP\tboardingInServiceAreaPt\talightingInServiceAreaPt"); writer.newLine();
			
				ana.reset(0);
				reader.readFile(eventsFile);
				ana.writeToFile(writer, scenarioId);			
		
			writer.close();	
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.print("... done ...");		
	}

	private void writeToFile(BufferedWriter writer, String scenarioId) throws IOException {
		writer.write(scenarioId + "\t" + this.numberOfTripsServedByPt + "\t" + this.minibusesWithAtLeastOneTrip.size() + "\t" + this.kmTravelledByMinibuses + "\t" + this.passengerKmMinibus + "\t" + this.numberOfPassengersUsingPtAndMinibus
				+ "\t" + this.boardingInServiceAreaP + "\t" + this.alightingInServiceAreaP + "\t" + this.boardingInServiceAreaPt + "\t" + this.alightingInServiceAreaPt); writer.newLine();
		writer.flush();
	}

	@Override
	public void reset(int iteration) {
		this.numberOfTripsServedByPt = 0;
		this.minibusesWithAtLeastOneTrip = new TreeSet<>();
		this.kmTravelledByMinibuses = 0.0;
		this.passengerKmMinibus = 0.0;
		this.vehId2NumberOfPassengers = new HashMap<>();
		this.numberOfPassengersUsingPtAndMinibus = 0;
		this.agentId2ModeAlreadyUsed = new HashMap<>();
		this.vehId2StopIdMap = new HashMap<>();
		
		this.boardingInServiceAreaPt = 0;
		this.alightingInServiceAreaPt = 0;
		this.boardingInServiceAreaP = 0;
		this.alightingInServiceAreaP = 0;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().contains(this.ptIdentifier)){
			if(!event.getPersonId().toString().contains(this.ptIdentifier)){
				this.numberOfTripsServedByPt++;
				
				if (!this.agentId2ModeAlreadyUsed.containsKey(event.getPersonId())) {
					this.agentId2ModeAlreadyUsed.put(event.getPersonId(), this.ptIdentifier);
				} else {
					if (this.agentId2ModeAlreadyUsed.get(event.getPersonId()).equalsIgnoreCase(this.pIdentifier)) {
						this.numberOfPassengersUsingPtAndMinibus++;
					} else {
						this.agentId2ModeAlreadyUsed.put(event.getPersonId(), this.ptIdentifier);
					}
				}
			
				if (stopIdInServiceArea(this.vehId2StopIdMap.get(event.getVehicleId()))) {
					this.boardingInServiceAreaPt++;
				}
				
			}
		}
		
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if (this.vehId2NumberOfPassengers.get(event.getVehicleId()) == null) {
				this.vehId2NumberOfPassengers.put(event.getVehicleId(), 0);
			}
			
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.minibusesWithAtLeastOneTrip.add(event.getVehicleId());

				this.vehId2NumberOfPassengers.put(event.getVehicleId(), this.vehId2NumberOfPassengers.get(event.getVehicleId()) + 1);
				
				if (!this.agentId2ModeAlreadyUsed.containsKey(event.getPersonId())) {
					this.agentId2ModeAlreadyUsed.put(event.getPersonId(), this.pIdentifier);
				} else {
					if (this.agentId2ModeAlreadyUsed.get(event.getPersonId()).equalsIgnoreCase(this.ptIdentifier)) {
						this.numberOfPassengersUsingPtAndMinibus++;
					} else {
						this.agentId2ModeAlreadyUsed.put(event.getPersonId(), this.pIdentifier);
					}
				}
				
				if (stopIdInServiceArea(this.vehId2StopIdMap.get(event.getVehicleId()))) {
					this.boardingInServiceAreaP++;
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.vehId2NumberOfPassengers.put(event.getVehicleId(), this.vehId2NumberOfPassengers.get(event.getVehicleId()) - 1);
				if (stopIdInServiceArea(this.vehId2StopIdMap.get(event.getVehicleId()))) {
					this.alightingInServiceAreaP++;
				}
			}
		}
		
		if(event.getVehicleId().toString().contains(this.ptIdentifier)){
			if(!event.getPersonId().toString().contains(this.ptIdentifier)){
				if (stopIdInServiceArea(this.vehId2StopIdMap.get(event.getVehicleId()))) {
					this.alightingInServiceAreaPt++;
				}
			}
		}
		
	}


	private boolean stopIdInServiceArea(Id<TransitStopFacility> stopId) {

		TransitStopFacility stop = this.transitSchedule.getFacilities().get(stopId);
		
		if (stop.getCoord().getX() < this.minX) {
			return false;
		}
		
		if (stop.getCoord().getX() > this.maxX) {
			return false;
		}
		
		if (stop.getCoord().getY() < this.minY) {
			return false;
		}
		
		if (stop.getCoord().getY() > this.maxY) {
			return false;
		}
		
		return true;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			double linkLength = this.network.getLinks().get(event.getLinkId()).getLength();
			this.kmTravelledByMinibuses += linkLength;
			this.passengerKmMinibus += this.vehId2NumberOfPassengers.get(event.getVehicleId()) * linkLength;
		}		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehId2StopIdMap.put(event.getVehicleId(), event.getFacilityId());		
	}
	
}
