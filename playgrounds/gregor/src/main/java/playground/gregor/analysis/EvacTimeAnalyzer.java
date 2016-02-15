/* *********************************************************************** *
 * project: org.matsim.*
 * GRIPSAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class EvacTimeAnalyzer implements PersonDepartureEventHandler, LinkEnterEventHandler {

	private static Set<Id<Link>> ends = new HashSet<>();

	static {
		ends.add(Id.createLinkId("4352"));
		ends.add(Id.createLinkId("104352"));
		ends.add(Id.createLinkId("1057"));
		ends.add(Id.createLinkId("101057"));
	}

	private final Map<Id<Vehicle>, PersonDepartureEvent> vehs = new HashMap<>();
	private final List<Measurement> ms = new ArrayList<Measurement>();
	double time = 3 * 3600;
	int carOnRoad;
	int carArrived;
	int carDeparted;

	public static void main(String[] args) throws IOException {
		String events = "/Users/laemmel/devel/padang/output/ITERS/it.100/100.events.xml.gz";
//		String events = "/Volumes/data/svn/runs-svn/run1010/output/ITERS/it.500/500.events.txt.gz";
		EventsManagerImpl m = new EventsManagerImpl();
		EvacTimeAnalyzer handl = new EvacTimeAnalyzer();
		m.addHandler(handl);
		new EventsReaderXMLv1(m).parse(events);
//		new TxtEventsFileReader(m).runEventsFile(events);
		BufferedWriter w = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/padang/analysis/CT")));
		System.out.println(handl);
		w.append(handl.toString());
		w.close();
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Measurement m : this.ms) {
			buf.append(m.time - 3600 * 3);
			buf.append(" ");
			buf.append(m.carDeparted);
			buf.append(" ");
			buf.append(m.carArrived);
			buf.append(" ");
			buf.append(m.carOnRoad);
			buf.append("\n");
		}

		return buf.toString();
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

//	@Override
//	public void handleEvent(PersonArrivalEvent event) {
//		if (event.getTime() > this.time) {
//			report();
//			this.time = event.getTime();
//		}
//
//
//			this.carArrived++;
//			this.carOnRoad--;
//
//
//	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLinkId().toString().contains("el")) {
			return;
		}
		if (event.getTime() > this.time) {
			report();
			this.time = event.getTime();
		}
		vehs.put(Id.create(event.getPersonId(), Vehicle.class), event);
		this.carDeparted++;
		this.carOnRoad++;


	}

	private void report() {
		Measurement m = new Measurement();
		m.time = this.time;
		m.carArrived = this.carArrived;
		m.carDeparted = this.carDeparted;
		m.carOnRoad = this.carOnRoad;
		this.ms.add(m);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getTime() > this.time) {
			report();
			this.time = event.getTime();
		}
		if (event.getLinkId().toString().contains("el") || ends.contains(event.getLinkId())) {
			Id<Vehicle> id = event.getVehicleId();
			PersonDepartureEvent ev = this.vehs.remove(id);
			if (ev != null) {
				this.carArrived++;
				this.carOnRoad--;
			}
		}
	}

	private static class Measurement {
		double time;

		int carOnRoad;

		int carArrived;

		int carDeparted;
	}

}
