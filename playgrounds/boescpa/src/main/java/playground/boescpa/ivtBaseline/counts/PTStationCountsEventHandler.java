/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline.counts;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Counts for selected train stations the number of passengers entering pt vehicles or leaving pt vehicles.
 *
 * @author boescpa
 */
public class PTStationCountsEventHandler implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private final Map<Id<Vehicle>, Id<TransitStopFacility>> vehStops = new HashMap<>();
	private final Set<Id<Person>> transitDrivers = new HashSet<>();
	private final Set<Id<Vehicle>> transitVehicles = new HashSet<>();
	private final double countsScaleFactor;
	private Scenario scenario;


	// Key String: General name of station (e.g. Zurich main station)
	// Tuple first int array: 24h for each hour the counted people entering vehicles.
	// Tuple second int array: 24h for each hour the counted people leaving vehicles.
	private final Map<String, Tuple<double[],double[]>> stationsToMonitor = new HashMap<>();
	// Key String: General name of station (e.g. Zurich main station)
	// Set String: pt stops to monitor under that general name
	private final Map<String, Set<Id<TransitStopFacility>>> ptStopsToMonitor = new HashMap<>();
	// Key String: All pt stops to monitor.
	// Tuple first Integer array: 24h for each hour the simulation counted people entering vehicles.
	// Tuple second Integer array: 24h for each hour the simulation counted people leaving vehicles.
	private final HashMap<Id<TransitStopFacility>, Tuple<int[],int[]>> ptCounts = new HashMap<>();

	@Inject
	private PTStationCountsEventHandler(@Named("pathToPTStationsToMonitor") final String pathToStationsList, Config config, Scenario scenario) {
		setStationsToMonitor(pathToStationsList);
		this.countsScaleFactor = config.ptCounts().getCountsScaleFactor();
		this.scenario = scenario;
	}

	private void setStationsToMonitor(final String pathToStationsList) {
		this.stationsToMonitor.clear();
		BufferedReader stationReader = IOUtils.getBufferedReader(pathToStationsList);
		try {
			stationReader.readLine(); // read header: stationNameAsInMATSimSchedule, 24 countVolumesEntering, 24 countVolumesLeaving
			String line = stationReader.readLine();
			while (line != null) {
				String[] lineElements = line.split(";");
				String stationName = lineElements[0].trim();
				double[] countVolumesEntering = new double[24];
				double[] countVolumesLeaving = new double[24];
				for (int i = 0; i < 24; i++) {
					countVolumesEntering[i] = Double.parseDouble(lineElements[1 + i]);
				}
				for (int i = 0; i < 24; i++) {
					countVolumesLeaving[i] = Double.parseDouble(lineElements[25 + i]);
				}
				this.stationsToMonitor.put(stationName, new Tuple<>(countVolumesEntering, countVolumesLeaving));
				line = stationReader.readLine();
			}
			stationReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset(int iteration) {
		// if not done already, initialize ptStopsToMonitor
		if (this.ptStopsToMonitor.isEmpty()) {
			for (String stationName : this.stationsToMonitor.keySet()) {
				Set<Id<TransitStopFacility>> ptStopsPerStation = new HashSet<>();
				for (TransitStopFacility transitStopFacility : this.scenario.getTransitSchedule().getFacilities().values()) {
					if (transitStopFacility.getName().equals(stationName)) {
						ptStopsPerStation.add(transitStopFacility.getId());
					}
				}
				this.ptStopsToMonitor.put(stationName, ptStopsPerStation);
			}
		}

		// reset ptStationCounter
		ptCounts.clear();
		for (String stationName : ptStopsToMonitor.keySet()) {
			for (Id<TransitStopFacility> facilityId : ptStopsToMonitor.get(stationName)) {
				ptCounts.put(facilityId, new Tuple<>(new int[24], new int[24]));
			}
		}
		vehStops.clear();
		transitDrivers.clear();
		transitVehicles.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());
		this.transitVehicles.add(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (!this.transitVehicles.contains(event.getVehicleId()) || !this.ptCounts.keySet().contains(event.getFacilityId())) {
			return; // ignore non-transit vehicles and vehicles stopping at non-monitoring stops
		}
		vehStops.put(event.getVehicleId(), event.getFacilityId());
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		vehStops.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !vehStops.keySet().contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-monitored vehicles
		}
		Id<TransitStopFacility> facilityId = vehStops.get(event.getVehicleId());
		int hourOfDay = (int)Math.floor(event.getTime()/3600);
		hourOfDay = hourOfDay < 24 ? hourOfDay : hourOfDay - 24;
		ptCounts.get(facilityId).getFirst()[hourOfDay]++;
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !vehStops.keySet().contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-monitored vehicles
		}
		Id<TransitStopFacility> facilityId = vehStops.get(event.getVehicleId());
		int hourOfDay = (int)Math.floor(event.getTime()/3600);
		hourOfDay = hourOfDay < 24 ? hourOfDay : hourOfDay - 24;
		ptCounts.get(facilityId).getSecond()[hourOfDay]++;
	}

	public void write(String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			// write file head
			writer.write("stationId\thour\tmatsimVolumeEntering\tcountVolumeEntering\trelativeErrorEntering\tmatsimVolumeLeaving\tcountVolumeLeaving\trelativeErrorLeaving");
			writer.newLine();
			// write content
			for (String station : stationsToMonitor.keySet()) {
				for (int i = 0; i < 24; i++) {
					double matsimVolumeEntering = 0;
					double matsimVolumeLeaving = 0;
					for (Id<TransitStopFacility> facilityId : ptStopsToMonitor.get(station)) {
						matsimVolumeEntering += ptCounts.get(facilityId).getFirst()[i]*countsScaleFactor;
						matsimVolumeLeaving += ptCounts.get(facilityId).getSecond()[i]*countsScaleFactor;
					}
					double countVolumeEntering = stationsToMonitor.get(station).getFirst()[i];
					double countVolumeLeaving = stationsToMonitor.get(station).getSecond()[i];
					double relativeEntering = countVolumeEntering > 0 ? matsimVolumeEntering/countVolumeEntering : matsimVolumeEntering*100;
					double relativeLeaving = countVolumeLeaving > 0 ? matsimVolumeLeaving/countVolumeLeaving : matsimVolumeLeaving*100;
					writer.write(station + "\t");
					writer.write(i + "\t");
					writer.write(matsimVolumeEntering + "\t" + countVolumeEntering + "\t" + relativeEntering + "\t");
					writer.write(matsimVolumeLeaving + "\t" + countVolumeLeaving + "\t" + relativeLeaving);
					writer.newLine();
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
