/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.thelma.psl;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.VehicleType;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;

/**
 * The goal is to output the parking times and energy consumptions based on a
 * events file.
 * 
 * The output format is: agentId, startParking, endParking, linkId (where
 * parked), actTypeOfActivity, energyConsumptionsInJoules (for previous trip).
 * 
 * @author wrashid
 * 
 */

public class ExportParkingTimesAndEnergyConsumptions {

	public static void main(String[] args) {

		String eventsFile = "C:/tmp2/matsim2030/run.3.90.events.xml.gz";
		String networkFile = "C:/tmp2/matsim2030/multimodalNetwork2010final.xml.gz";
		EventsManager events = EventsUtils.createEventsManager();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();

		// addActivityFilter(parkingTimesPlugin);

		events.addHandler(parkingTimesPlugin);

		EnergyConsumptionPlugin energyConsumptionPlugin = getEnergyConsumptionPlugin(networkFile);

		events.addHandler(energyConsumptionPlugin);

		// EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);

		// reader.readFile(eventsFile);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		parkingTimesPlugin.closeLastAndFirstParkingIntervals();

		printParkingTimesAndEnergyConsumptionTable(parkingTimesPlugin,
				energyConsumptionPlugin);
	}

	private static void addActivityFilter(ParkingTimesPlugin parkingTimesPlugin) {
		LinkedList<String> actTypesFilter = new LinkedList<String>();
		actTypesFilter.add("w");
		parkingTimesPlugin.setActTypesFilter(actTypesFilter);
	}

	private static void printParkingTimesAndEnergyConsumptionTable(
			ParkingTimesPlugin parkingTimesPlugin,
			EnergyConsumptionPlugin energyConsumptionPlugin) {
		System.out
				.println("agentId\tstartParking\tendParking\tlinkId\tactType\tenergyConsumptionsInJoules\ttripLengthInMeters");
		for (Id<Person> personId : parkingTimesPlugin.getParkingTimeIntervals()
				.getKeySet()) {
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimesPlugin
					.getParkingTimeIntervals().get(personId);
			LinkedList<Double> energyConsumptionOfLegs = energyConsumptionPlugin
					.getEnergyConsumptionOfLegs().get(personId);
			LinkedList<Double> tripLengthOfLegs = energyConsumptionPlugin
					.getTripLengthOfLegsInMeters().get(personId);

			for (int i = 0; i < parkingIntervals.size(); i++) {
				if (!personId.toString().contains("pt")) {
					System.out.println(personId
							+ "\t"
							+ GeneralLib
									.projectTimeWithin24Hours(parkingIntervals
											.get(i).getArrivalTime())
							+ "\t"
							+ GeneralLib
									.projectTimeWithin24Hours(parkingIntervals
											.get(i).getDepartureTime())
							+ "\t"
							+ parkingIntervals.get(i).getLinkId()
							+ "\t"
							+ parkingIntervals.get(i)
									.getActTypeOfFirstActDuringParking() + "\t"
							+ energyConsumptionOfLegs.get(i) + "\t"
							+ tripLengthOfLegs.get(i));
				}
			}
		}
	}

	private static EnergyConsumptionPlugin getEnergyConsumptionPlugin(
			String networkFile) {
		EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelPSL(
				140);
		LinkedListValueHashMap<Id<Vehicle>, Vehicle> vehicles = new LinkedListValueHashMap<>();
		vehicles.put(Id.create(Vehicle.getPlaceholderForUnmappedPersonIds(), Vehicle.class),
				new PlugInHybridElectricVehicle(Id.create(1, VehicleType.class)));
		Network network = GeneralLib.readNetwork(networkFile);
		EnergyConsumptionPlugin energyConsumptionPlugin = new EnergyConsumptionPlugin(
				energyConsumptionModel, vehicles, network);
		return energyConsumptionPlugin;
	}

}
