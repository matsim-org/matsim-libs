/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.lib.tools.scenarioUtils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Changes departure frequencies in a given schedule...
 *
 * @author boescpa
 */
public class PTDepartureFrequencyModification {

	public static void main(String[] args) {
		if (args.length < 4 || args.length > 4) {
			System.out.println("Wrong number of arguments. Will abort.");
			return;
		}

		// Load scenario and schedule:
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		final int desiredFrequency = Integer.parseInt(args[1])*60;

		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(scenario.getConfig().transit().getTransitScheduleFile());
		TransitSchedule schedule = scenario.getTransitSchedule();
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(scenario.getConfig().transit().getVehiclesFile());
		Vehicles vehicles = scenario.getTransitVehicles();

		// Increase frequency of departures:
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				// Sort departures in increasing departure times:
				List<Departure> departuresUnordered = new ArrayList<>();
				departuresUnordered.addAll(route.getDepartures().values());
				List<Departure> departuresOrderedDepartureTime = new ArrayList<>();
				while(departuresUnordered.size() > 0) {
					Departure earliesDeparture = null;
					for (Departure departure : departuresUnordered) {
						if(earliesDeparture == null || departure.getDepartureTime() < earliesDeparture.getDepartureTime()) {
							earliesDeparture = departure;
						}
					}
					departuresOrderedDepartureTime.add(earliesDeparture);
					departuresUnordered.remove(earliesDeparture);
				}
				// If departure time i+1 is less than 10 minutes from departure time i, replace frequency by the new set.
				for (int i = 0; i < departuresOrderedDepartureTime.size() - 1; i++) {
					int j = 0;
					while ((i + 1 + j < departuresOrderedDepartureTime.size())
							&& ((departuresOrderedDepartureTime.get(i + 1 + j).getDepartureTime()
								- departuresOrderedDepartureTime.get(i + j).getDepartureTime()) < 10*60)) {
						route.removeDeparture(departuresOrderedDepartureTime.get(i + j));
						vehicles.removeVehicle(departuresOrderedDepartureTime.get(i + j).getVehicleId());
						j++;
					}
					if (j > 0) {
						j--;
						route.removeDeparture(departuresOrderedDepartureTime.get(i + 1 + j));
						Vehicle vehicleOriginal = vehicles.getVehicles().get(departuresOrderedDepartureTime.get(i + 1 + j).getVehicleId());
						vehicles.removeVehicle(vehicleOriginal.getId());

						// Until here old departures (and vehicles) removed, now build new departures and vehicles...
						Departure departureOriginal = departuresOrderedDepartureTime.get(i);
						TransitScheduleFactory factory = schedule.getFactory();
						VehiclesFactory vehiclesFactory = vehicles.getFactory();
						double earliestDeparture = departuresOrderedDepartureTime.get(i).getDepartureTime();
						double latestDeparture = departuresOrderedDepartureTime.get(i + 1 + j).getDepartureTime();
						double currentDeparture = earliestDeparture;
						while (currentDeparture < latestDeparture) {
							// new departure
							Departure newDeparture = factory.createDeparture(
									Id.create(departureOriginal.getId().toString() + "newDepAt" + currentDeparture, Departure.class),
									currentDeparture
							);
							//	new vehicle
							Vehicle newVehicle;
							{
								vehicles.removeVehicle(vehicleOriginal.getId());
								newVehicle = vehiclesFactory.createVehicle(
										Id.create(departureOriginal.getVehicleId().toString() + "newDepAt" + currentDeparture, Vehicle.class),
										vehicleOriginal.getType()
								);
								vehicles.addVehicle(newVehicle);
							}
							newDeparture.setVehicleId(newVehicle.getId());
							route.addDeparture(newDeparture);
							// increment departure time:
							currentDeparture += desiredFrequency;
						}
					}
					i = i + j;
				}
			}
		}

		new TransitScheduleWriter(schedule).writeFile(args[2]);
		new VehicleWriterV1(vehicles).writeFile(args[3]);

		// **************** Validate final schedule ****************
		// validate vehicles
		boolean allVehiclesPresent = true;
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					if (!vehicles.getVehicles().containsKey(departure.getVehicleId())) {
						allVehiclesPresent = false;
					}
				}
			}
		}
		if (allVehiclesPresent) {
			System.out.println("All vehicles present.");
		} else {
			System.out.println("ERROR: NOT ALL VEHICLES PRESENT!!!");
			return;
		}
		// validate schedule
		try {
			TransitScheduleValidator.main(new String[]{args[2], scenario.getConfig().network().getInputFile()});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
