/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis.zonal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.vrpagent.AbstractTaskEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.collections.Tuple;

import com.opencsv.CSVWriter;

public class ZonalIdleVehicleXYVisualiser
		implements TaskStartedEventHandler, TaskEndedEventHandler, IterationEndsListener {

	private final String mode;
	private final DrtZonalSystem zonalSystem;
	private final MatsimServices services;

	private final Map<DrtZone, LinkedList<Tuple<Double, Integer>>> zoneEntries = new HashMap<>();

	public ZonalIdleVehicleXYVisualiser(MatsimServices services, String mode, DrtZonalSystem zonalSystem) {
		this.services = services;
		this.mode = mode;
		this.zonalSystem = zonalSystem;
		initEntryMap();
	}

	private void initEntryMap() {
		for (DrtZone z : zonalSystem.getZones().values()) {
			LinkedList<Tuple<Double, Integer>> list = new LinkedList<>();
			list.add(new Tuple<>(0d, 0));
			zoneEntries.put(z, list);
		}
	}

	@Override
	public void handleEvent(TaskStartedEvent event) {
		handleEvent(event, zone -> {
			LinkedList<Tuple<Double, Integer>> zoneTuples = zoneEntries.get(zone);
			Integer oldNrOfVeh = zoneTuples.getLast().getSecond();
			zoneTuples.add(new Tuple<>(event.getTime(), oldNrOfVeh + 1));
		});
	}

	@Override
	public void handleEvent(TaskEndedEvent event) {
		handleEvent(event, zone -> {
			LinkedList<Tuple<Double, Integer>> zoneTuples = zoneEntries.get(zone);
			Integer oldNrOfVeh = zoneTuples.getLast().getSecond();
			zoneTuples.add(new Tuple<>(event.getTime(), oldNrOfVeh - 1));
		});
	}

	private void handleEvent(AbstractTaskEvent event, Consumer<DrtZone> handler) {
		if (event.getDvrpMode().equals(mode) && event.getTaskType().equals(DrtStayTask.TYPE)) {
			DrtZone zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone != null) {
				handler.accept(zone);
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String filename = services.getControlerIO()
				.getIterationFilename(services.getIterationNumber(), mode + "_idleVehiclesPerZoneXY.csv");

		try {
			CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(filename)), ';', '"', '"', "\n");
			writer.writeNext(new String[] { "zone", "X", "Y", "time", "idleDRTVehicles" }, false);
			this.zoneEntries.forEach((zone, entriesList) -> {
				Coord c = zone.getCentroid();
				entriesList.forEach(entry -> writer.writeNext(
						new String[] { zone.getId(), "" + c.getX(), "" + c.getY(), "" + entry.getFirst(),
								"" + entry.getSecond() }, false));
			});

			writer.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	@Override
	public void reset(int iteration) {
		initEntryMap();
	}

}
