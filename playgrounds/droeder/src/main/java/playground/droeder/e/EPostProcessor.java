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
package playground.droeder.e;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.energy.validation.ValidationInfoReader;
import playground.dgrether.energy.validation.ValidationInformation;
import playground.droeder.e.ePlans.EVehiclesPlanReader;
import playground.droeder.e.eVehicles.EVehicles;
import playground.droeder.e.poi.PoiList;
import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.energy.EmobEnergyProfileReader;

/**
 * @author droeder
 *
 */
public class EPostProcessor implements LinkEnterEventHandler, LinkLeaveEventHandler,
							PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	public static final String IDENTIFIER = "emob_";
	private final static String DIR = "D:/VSP/svn/shared/volkswagen_internal/";
//	private static final String DIR = "/home/dgrether/shared-svn/projects/volkswagen_internal/";
	
	private final static String NET = DIR + "scenario/input/network-base_ext.xml.gz";
	private final static String DPROFILE = DIR + "Dokumente_MATSim_AP1und2/DrivingLookupTable_2011-11-25.txt";
	private final static String CPROFILE = DIR + "Dokumente_MATSim_AP1und2/ChargingLookupTable_2011-11-30.txt";
	private final static String EVENTS = DIR + "matsimOutput/congestion/ITERS/it.0/0.events.xml.gz";
	private final static String POIINFO = DIR + "scenario/input/poiInfo.xml";
	private final static String VEHICLEPLANS = DIR + "scenario/input/testAppointments.txt";

	private final static String OUTDIR = DIR + "matsimOutput/congestion/ITERS/it.0/energy/";

	private EVehicles vehicles;

	public EPostProcessor(EVehicles vehicles){
		this.vehicles = vehicles;
	}

	@Override
	public void reset(int iteration) {
		// do nothing
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.vehicles.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.vehicles.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.vehicles.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.vehicles.handleEvent(event);
	}
	
	public static void main(String[] args){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(NET );
		
		DisChargingProfiles dp = EmobEnergyProfileReader.readDisChargingProfiles(DPROFILE);
		ChargingProfiles cp = EmobEnergyProfileReader.readChargingProfiles(CPROFILE);
		ValidationInformation info = new ValidationInfoReader().readFile(POIINFO);
		
		
		EVehicles vehicles = new EVehicles(cp, dp, sc.getNetwork(), new PoiList(info, 3600));
		
		new EVehiclesPlanReader(vehicles).read(VEHICLEPLANS);
		
		EventsManager manager = EventsUtils.createEventsManager();
		EPostProcessor processor = new EPostProcessor(vehicles);
		manager.addHandler(processor);
		new MatsimEventsReader(manager).readFile(EVENTS );
		vehicles.dumpData(OUTDIR, info);
	}

}
