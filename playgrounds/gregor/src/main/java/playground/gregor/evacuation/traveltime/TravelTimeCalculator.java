/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.evacuation.traveltime;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class TravelTimeCalculator implements AgentArrivalEventHandler {
	
	int count = 0;
	double time = 0;
	
	public static void main(String [] args) {
		String events = "/home/laemmel/devel/allocation/output/ITERS/it.150/150.events.xml.gz";
		TravelTimeCalculator tt = new TravelTimeCalculator();
		
		EventsManager ev = EventsUtils.createEventsManager();
		ev.addHandler(tt);
		new EventsReaderXMLv1(ev).parse(events);
		System.out.println(tt.time/tt.count);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.count++;
		this.time += event.getTime()-3*3600;
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

}
