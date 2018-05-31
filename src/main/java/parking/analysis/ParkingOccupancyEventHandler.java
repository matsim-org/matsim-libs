/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package parking.analysis;

import com.google.common.primitives.Ints;
import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import parking.ParkingZone;
import parking.ZonalLinkParkingInfo;
import parking.capacityCalculation.LinkParkingCapacityCalculator;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ParkingOccupancyEventHandler implements PersonDepartureEventHandler, ActivityEndEventHandler {

	private ZonalLinkParkingInfo parkingInfo;
	private LinkParkingCapacityCalculator calculator;
	private Network network;
	private final double storageCapacityFactor;
	private Map<Id<ParkingZone>, int[]> zoneoccupancyPerBin = new HashMap<>();
	private Map<Id<ParkingZone>, Integer> capacityAtSimulationStart = new HashMap<>();
	private int bins;
	private Map<Id<Person>, String> lastActivityType = new HashMap<>();
	private boolean initialized = false;
	
	public ParkingOccupancyEventHandler(ZonalLinkParkingInfo parkingInfo, LinkParkingCapacityCalculator calculator,
										Network network, double simEndTime, double storageCapacityFactor) {
		this.storageCapacityFactor = storageCapacityFactor;
		this.parkingInfo = parkingInfo;
		this.calculator = calculator;
		this.network = network;
		bins = (int) (simEndTime / 900);
		for (Id<ParkingZone> zone : parkingInfo.getParkingZones().keySet()) {
			zoneoccupancyPerBin.put(zone,new int[bins]);
		}
	}
	
	@Inject
	public ParkingOccupancyEventHandler(ZonalLinkParkingInfo parkingInfo, LinkParkingCapacityCalculator calculator,
										Network network, Config config, EventsManager events) {
		events.addHandler(this);
		this.storageCapacityFactor = config.qsim().getStorageCapFactor();
		this.parkingInfo = parkingInfo;
		this.calculator = calculator;
		this.network = network;
		bins = (int) (config.qsim().getEndTime() / 900);
		for (Id<ParkingZone> zone : parkingInfo.getParkingZones().keySet()) {
			zoneoccupancyPerBin.put(zone,new int[bins]);
		}
	}
	
	@Override
	public void reset(int iteration) {

		zoneoccupancyPerBin.clear();
		for (Entry<Id<ParkingZone>, ParkingZone> zone : parkingInfo.getParkingZones().entrySet()) {
			zoneoccupancyPerBin.put(zone.getKey(), new int[bins]);
			if (!initialized) {
				capacityAtSimulationStart.put(zone.getKey(), (int) zone.getValue().getZoneParkingCapacity());

			}
		}
		initialized = true;

		lastActivityType.clear();

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			String lastAct = String.valueOf(lastActivityType.get(event.getPersonId()));
			if (lastAct.equals("car interaction")) {
				ParkingZone zone = parkingInfo.getParkingZone(network.getLinks().get(event.getLinkId()));
				if (zone != null) {
					this.zoneoccupancyPerBin.get(zone.getId())[getBin(event.getTime())]++;
				}
			}
		} else if (event.getLegMode().equals(TransportMode.egress_walk)) {
			String lastAct = String.valueOf(lastActivityType.get(event.getPersonId()));
			if (lastAct.equals("car interaction")) {
				ParkingZone zone = parkingInfo.getParkingZone(network.getLinks().get(event.getLinkId()));
				if (zone != null) {
					this.zoneoccupancyPerBin.get(zone.getId())[getBin(event.getTime())]--;
				}

			}
		}
	}

	private int getBin(double time) {
		int b = Math.floorDiv((int) time, 900);
		if (b > bins - 1) b = bins - 1;
		return b;
		
	}

	public void writeParkingOccupancyStats(String abs, String rel) {
		BufferedWriter bw = IOUtils.getBufferedWriter(abs);
		BufferedWriter bwr = IOUtils.getBufferedWriter(rel);
		try {
			bw.write("Zone;Capacity;intialOccupancy");
			bwr.write("Zone;Capacity;intialOccupancy");

			for (int i = 0; i<bins; i++) {
				bw.write(";"+Time.writeTime(i*900));
				bwr.write(";" + Time.writeTime(i * 900));
			}
			for (Entry<Id<ParkingZone>, int[]> e : this.zoneoccupancyPerBin.entrySet()) {
				bw.newLine();
				bwr.newLine();
				final MutableInt parkingCapacity = new MutableInt();
				this.parkingInfo.getParkingZones().get(e.getKey()).getLinksInZone().forEach(lid -> parkingCapacity.add(calculator.getLinkCapacity(network.getLinks().get(lid))));
				int cap = (int) (parkingCapacity.intValue() * storageCapacityFactor);
				int initialOcc = cap - this.capacityAtSimulationStart.get(e.getKey());

				bw.write(e.getKey() + ";" + cap + ";");
				bwr.write(e.getKey() + ";" + cap + ";");

				int[] sum = new int[e.getValue().length];
				for (int i = 0 ; i< e.getValue().length;i++) {
					int j = (i == 0) ? 0 : i - 1;
					sum[i] = sum[j] + e.getValue()[i];
				}
				int min = Math.abs(Ints.min(sum));
				int io = Math.max(min, initialOcc);
				bw.write(Integer.toString(io));
				bwr.write(Integer.toString(io));
				for (int i = 0; i < sum.length; i++) {
					bw.write(";" + sum[i]);
				}
				double fsum = io;
				for (int i = 0; i < e.getValue().length; i++) {
					fsum += e.getValue()[i];
					double relOcc = fsum / (double) cap;
					bwr.write(";" + String.format("%.2f", relOcc));
				}

			}
			bw.flush();
			bw.close();
            bwr.flush();
            bwr.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.lastActivityType.put(event.getPersonId(), event.getActType());
	}
}
