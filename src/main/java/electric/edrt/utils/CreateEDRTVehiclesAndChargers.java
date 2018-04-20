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

package electric.edrt.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vsp.ev.EvUnitConversions;
import org.matsim.vsp.ev.data.BatteryImpl;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ChargerImpl;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.data.ElectricVehicleImpl;
import org.matsim.vsp.ev.data.file.ChargerWriter;
import org.matsim.vsp.ev.data.file.ElectricVehicleWriter;

/**
 * @author  jbischoff
 * This is an example script to create electric drt vehicle files. The vehicles are distributed along defined depots.
 *
 */
public class CreateEDRTVehiclesAndChargers {
	
	static final int BATTERY_CAPACITY_KWH = 30;
	static final int MIN_START_CAPACITY_KWH = 10;
	static final int MAX_START_CAPACITY_KWH = 30;
	
	static final int CHARGINGPOWER_KW = 50;
	
	static final int SEATS = 8;
	
	static final String NETWORKFILE = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/drt_test_Scenarios/BS_DRT/input/network/modifiedNetwork.xml.gz";
	static final String E_VEHICLE_FILE = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/drt_test_Scenarios/BS_DRT/input/edrt/e-vehicles_bs_100.xml";
	static final String DRT_VEHICLE_FILE = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/drt_test_Scenarios/BS_DRT/input/edrt/e-drt_bs_100.xml";
	static final String CHARGER_FILE = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/drt_test_Scenarios/BS_DRT/input/edrt/chargers_bs_100.xml";
	
	static final double OPERATIONSTARTTIME = 0.; //t0
	static final double OPERATIONENDTIME = 30*3600.;	//t1
	
	static final double FRACTION_OF_CHARGERS_PER_DEPOT = 0.4; //relative number of chargers to numbers of vehicle at location
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Map<Id<Link>,Integer> depotsAndVehicles = new HashMap<>();
		depotsAndVehicles.put(Id.createLinkId(40158), 25); //BS HBF
		depotsAndVehicles.put(Id.createLinkId(8097), 25); //Zentrum SO
		depotsAndVehicles.put(Id.createLinkId(13417), 25); //Zentrum N
		depotsAndVehicles.put(Id.createLinkId(14915), 25); //Flugplatz
		new CreateEDRTVehiclesAndChargers().run(depotsAndVehicles);
		
		
	}
	
	private void run(Map<Id<Link>,Integer> depotsAndVehicles) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
		List<Vehicle> vehicles = new ArrayList<>();
		List<ElectricVehicle> evehicles = new ArrayList<>();
		List<Charger> chargers = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
		for (Entry<Id<Link>, Integer> e : depotsAndVehicles.entrySet()) {
			Link startLink;
			startLink =  scenario.getNetwork().getLinks().get(e.getKey());
			if(!startLink.getAllowedModes().contains(TransportMode.car)) {
				throw new RuntimeException("StartLink " + startLink.getId().toString() + " does not allow car mode.");
			}
			for (int i = 0; i< e.getValue();i++){
				
				Vehicle v = new VehicleImpl(Id.create("taxi_"+startLink.getId().toString()+"_"+i, Vehicle.class), startLink, SEATS, OPERATIONSTARTTIME, OPERATIONENDTIME);
				vehicles.add(v);
				double initialSoc_kWh = MIN_START_CAPACITY_KWH + random.nextDouble()*(MAX_START_CAPACITY_KWH-MIN_START_CAPACITY_KWH); 
				ElectricVehicle ev = new ElectricVehicleImpl(Id.create(v.getId(), ElectricVehicle.class), new BatteryImpl(BATTERY_CAPACITY_KWH * EvUnitConversions.J_PER_kWh,
						initialSoc_kWh * EvUnitConversions.J_PER_kWh));
				evehicles.add(ev);
				
			}
			int chargersPerDepot = (int) (e.getValue()*FRACTION_OF_CHARGERS_PER_DEPOT);
			Charger charger = new ChargerImpl(Id.create("charger_"+startLink.getId(),Charger.class), CHARGINGPOWER_KW*EvUnitConversions.W_PER_kW,chargersPerDepot , startLink);
			chargers.add(charger);
			
		}
		new VehicleWriter(vehicles).write(DRT_VEHICLE_FILE);
		new ElectricVehicleWriter(evehicles).write(E_VEHICLE_FILE);
		new ChargerWriter(chargers).write(CHARGER_FILE);
	}
	

}
