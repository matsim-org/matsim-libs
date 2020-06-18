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

import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.collections.Tuple;

import com.opencsv.CSVWriter;

public class ZonalIdleVehicleXYVisualiser implements ActivityEndEventHandler, ActivityStartEventHandler, IterationEndsListener {

	private final DrtZonalSystem zonalSystem;
	private final MatsimServices services;

	private final Map<String, LinkedList<Tuple<Double, Integer>>> zoneEntries = new HashMap<>();

	public ZonalIdleVehicleXYVisualiser(MatsimServices services, DrtZonalSystem zonalSystem) {
		this.services = services;
		services.getEvents().addHandler(this);
		this.zonalSystem = zonalSystem;
		initEntryMap();
	}

	private void initEntryMap() {
		for (String z : zonalSystem.getZones().keySet()) {
			LinkedList<Tuple<Double,Integer>> list = new LinkedList<>();
			list.add(new Tuple<>(0d, 0));
			zoneEntries.put(z, list);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)) {
			String zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone != null) {
				Integer oldNrOfVeh = zoneEntries.get(zone).getLast().getSecond();
				zoneEntries.get(zone).add(new Tuple<>(event.getTime(), oldNrOfVeh + 1));
			}
		}

		if (event.getActType().equals(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE)) {
			String zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone != null) {
				Integer oldNrOfVeh = zoneEntries.get(zone).getLast().getSecond();
				zoneEntries.get(zone).add(new Tuple<>(event.getTime(), oldNrOfVeh - 1));
			}
		}

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)) {
			String zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone != null) {
				Integer oldNrOfVeh = zoneEntries.get(zone).getLast().getSecond();
				zoneEntries.get(zone).add(new Tuple<>(event.getTime(), oldNrOfVeh - 1));
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String filename = services.getControlerIO().getIterationFilename(services.getIterationNumber(), "idleVehiclesPerZoneXY.csv");

		try {
			CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(filename)), ';', '"', '"', "\n");
			writer.writeNext(new String[]{"zone", "X", "Y", "time", "idleDRTVehicles"}, false);
			this.zoneEntries.forEach( (zone, entriesList) -> {
				Point p = zonalSystem.getZone(zone).getCentroid();
				double x = p.getX();
				double y = p.getY();
				entriesList.forEach(entry -> writer.writeNext(new String[]{zone, "" + x, "" + y, "" + entry.getFirst(), "" + entry.getSecond()}, false));
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
