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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Counts the daily passengers on a given link.
 *
 * This is a customized version of org.matsim.pt.counts.OccupancyAnalyzer by yChen and mrieser
 * @author boescpa
 */
public class PTCountsIVTBaseline implements IterationEndsListener, IterationStartsListener, LinkEnterEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private static final Logger log = Logger.getLogger(PTCountsIVTBaseline.class);

	private final Controler controler;
	private final Set<String> linksToMonitor;
	private final HashMap<String, Integer> ptCounts = new HashMap<>();
	private final Map<Id<Vehicle>, Integer> vehPassengers = new HashMap<>();
	private final Set<Id<Person>> transitDrivers = new HashSet<>();
	private final Set<Id<Vehicle>> transitVehicles = new HashSet<>();

	private boolean recordCounts;

	public PTCountsIVTBaseline(Controler controler, Set<String> linksToMonitor) {
		this.controler = controler;
		this.linksToMonitor = linksToMonitor;
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
		recordCounts = true;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.recordCounts) {
			write(controler.getControlerIO().getIterationFilename(event.getIteration(), "ptCounts.txt"));
		}
		this.recordCounts = false;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int countsInterval = this.controler.getConfig().counts().getWriteCountsInterval();
		if (event.getIteration() % countsInterval == 0) {
			this.reset(event.getIteration());
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (this.recordCounts) {
			this.transitDrivers.add(event.getDriverId());
			this.transitVehicles.add(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.recordCounts) {
			if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
				return; // ignore transit drivers or persons entering non-transit vehicles
			}
			Id<Vehicle> vehId = event.getVehicleId();
			Integer nPassengers = this.vehPassengers.get(vehId);
			this.vehPassengers.put(vehId, (nPassengers != null) ? (nPassengers + 1) : 1);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.recordCounts) {
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
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.recordCounts) {
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
	}

	private void write(String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			// write file head
			writer.write("linkId\tnumberOfPassengers");
			writer.newLine();
			// write content
			for (String linkId : ptCounts.keySet()) {
				writer.write(linkId + "\t");
				writer.write(ptCounts.get(linkId));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
