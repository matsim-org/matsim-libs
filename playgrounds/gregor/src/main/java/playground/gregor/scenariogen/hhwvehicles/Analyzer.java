package playground.gregor.scenariogen.hhwvehicles;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by laemmel on 10/11/15.
 */
public class Analyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final List<Measurement> ms = new ArrayList<Measurement>();
	double time = 0;
	int carOnRoad;
	int carArrived;
	int carDeparted;

	public static void main(String[] args) throws IOException {
		String events = args[0];
		String outp = args[1];
//		String events = "/Users/laemmel/arbeit/papers/2015/TRBwFZJ/hybridsim_trb2016/analysis/dirac-delta-vehicles/output/ITERS/it.0/0.events.xml.gz";
		EventsManagerImpl m = new EventsManagerImpl();
		Analyzer handl = new Analyzer();
		m.addHandler(handl);
		new EventsReaderXMLv1(m).parse(events);
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(outp)));
		System.out.println(handl);
		w.append(handl.toString());
		w.close();
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Measurement m : this.ms) {
			buf.append(m.time);
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

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getTime() > this.time) {
			report();
			this.time = event.getTime();
		}
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
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getTime() > this.time) {
			report();
			this.time = event.getTime();
		}
		this.carArrived++;
		this.carOnRoad--;

	}

	private static class Measurement {
		double time;
		int carOnRoad;
		int carArrived;
		int carDeparted;
	}

}
