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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

public class GRIPSAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler{

	double time = 0;
	int pedOnRoad;
	int carOnRoad;
	int pedArrived;
	int carArrived;
	int pedDeparted;
	int carDeparted;
	
	private final List<Measurement> ms = new ArrayList<Measurement>();
	
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
		if (event.getPersonId().toString().startsWith("car")) {
			this.carDeparted++;
			this.carOnRoad++;
		} else {
			this.pedDeparted++;
			this.pedOnRoad++;
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getTime() > this.time) {
			report();
			this.time = event.getTime();
		}
		
		if (event.getPersonId().toString().startsWith("car")) {
			this.carArrived++;
			this.carOnRoad--;
		} else {
			this.pedArrived++;
			this.pedOnRoad--;
		}
		
	}

	private void report() {
		Measurement m = new Measurement();
		m.time = this.time;
		m.carArrived = this.carArrived;
		m.carDeparted = this.carDeparted;
		m.carOnRoad = this.carOnRoad;
		m.pedArrived = this.pedArrived;
		m.pedDeparted = this.pedDeparted;
		m.pedOnRoad = this.pedOnRoad;
		this.ms.add(m);
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Measurement m : this.ms ) {
			buf.append(m.time);
			buf.append(" " );
			buf.append(m.carDeparted+m.pedDeparted);
			buf.append(" " );
			buf.append(m.carArrived+m.pedArrived);
			buf.append(" " );
			buf.append(m.carOnRoad+m.pedOnRoad);
			buf.append(" " );
			buf.append(m.carDeparted);
			buf.append(" " );
			buf.append(m.carArrived);
			buf.append(" " );
			buf.append(m.carOnRoad);
			buf.append(" " );
			buf.append(m.pedDeparted);
			buf.append(" " );
			buf.append(m.pedArrived);
			buf.append(" " );
			buf.append(m.pedOnRoad);
			buf.append("\n");
		}
		
		return buf.toString();
	}


	private static class Measurement {
		double time;
		int pedOnRoad;
		int carOnRoad;
		int pedArrived;
		int carArrived;
		int pedDeparted;
		int carDeparted;
	}
	
	public static void main (String [] args) throws IOException {
		String events = "/Users/laemmel/devel/hhw_hybrid/output/ITERS/it.100/100.events.xml.gz";
		EventsManagerImpl m = new EventsManagerImpl();
		GRIPSAnalyzer handl = new GRIPSAnalyzer();
		m.addHandler(handl);
		new EventsReaderXMLv1(m).parse(events);
		BufferedWriter w = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/hhw_hybrid/plots/ne")));
		System.out.println(handl);
		w.append(handl.toString());
		w.close();
	}

}
