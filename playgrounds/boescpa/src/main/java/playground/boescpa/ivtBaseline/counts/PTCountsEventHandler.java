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

package playground.boescpa.ivtBaseline.counts;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Counts the daily passengers on a given link.
 *
 * @author boescpa
 */
public class PTCountsEventHandler implements LinkEnterEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private static final Logger log = Logger.getLogger(PTCountsEventHandler.class);

	private final Map<Id<Vehicle>, Integer> vehPassengers = new HashMap<>();
	private final Set<Id<Person>> transitDrivers = new HashSet<>();
	private final Set<Id<Vehicle>> transitVehicles = new HashSet<>();
	private final HashMap<String, Integer> ptCounts = new HashMap<>();

	private final Set<String> linksToMonitor = new HashSet<>();

	@Inject
	private PTCountsEventHandler(@Named("pathToPTLinksToMonitor") final String pathToLinksList) {
		setLinksToMonitor(pathToLinksList);
	}

	private void setLinksToMonitor(final String pathToLinksList) {
		this.linksToMonitor.clear();
		BufferedReader linkReader = IOUtils.getBufferedReader(pathToLinksList);
		try {
			String line = linkReader.readLine();
			while (line != null) {
				this.linksToMonitor.add(line);
				line = linkReader.readLine();
			}
			linkReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset(int iteration) {
		ptCounts.clear();
		for (String linkId : linksToMonitor) {
			ptCounts.put(linkId, 0);
		}
		vehPassengers.clear();
		transitDrivers.clear();
		transitVehicles.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());
		this.transitVehicles.add(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles
		}
		Id<Vehicle> vehId = event.getVehicleId();
		Integer nPassengers = this.vehPassengers.get(vehId);
		this.vehPassengers.put(vehId, (nPassengers != null) ? (nPassengers + 1) : 1);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles
		}
		Id<Vehicle> vehId = event.getVehicleId();
		Integer nPassengers = this.vehPassengers.get(vehId);
		if (nPassengers != null) {
			this.vehPassengers.put(vehId, nPassengers - 1);
			if (this.vehPassengers.get(vehId) == 0) {
				this.vehPassengers.remove(vehId);
			}
		} else {
			log.fatal("negative passenger-No. in vehicle?");
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		if (!this.transitVehicles.contains(vehicleId)) {
			return; // ignore non-transit vehicles
		}
		String linkId = event.getLinkId().toString();
		if (ptCounts.keySet().contains(linkId)) {
			Integer count = ptCounts.get(linkId);
			Integer nPassengers = this.vehPassengers.get(vehicleId);
			if (nPassengers != null) {
				count += nPassengers;
			}
			ptCounts.put(linkId, count);
		}
	}

	public void write(String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			// write file head
			writer.write("linkId\tnumberOfPassengers");
			writer.newLine();
			// write content
			for (String linkId : ptCounts.keySet()) {
				writer.write(linkId + "\t");
				writer.write(ptCounts.get(linkId).toString());
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
