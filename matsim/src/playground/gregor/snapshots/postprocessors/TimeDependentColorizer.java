/* *********************************************************************** *
 * project: org.matsim.*
 * TimeDependentColorizer.java
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

package playground.gregor.snapshots.postprocessors;


import java.util.HashMap;

import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;

import playground.gregor.snapshots.writers.PositionInfo;




public class TimeDependentColorizer implements PostProcessorI, BasicAgentDepartureEventHandler, BasicAgentArrivalEventHandler, BasicAgentStuckEventHandler{

//	private final Plans plans;
	private final HashMap<String,EventAgent> agents = new HashMap<String,EventAgent>();
//	public TimeDependentColorizer(Plans plans) {
//		this.plans = plans;
//	}


	public String[] processEvent(String[] event){
		String id = event[0];
		EventAgent e = this.agents.get(id);
		int time =  (int) ((e.endtime-e.starttime) / 60);
		event[7] = Integer.toString(Math.min(time,255));
		return event;
	}

	public void processPositionInfo(PositionInfo pos) {
		EventAgent e = this.agents.get(pos.getAgentId().toString());
		int time =  (int) ((e.endtime-e.starttime) / 60);
		pos.setType(Math.min(time,255));
	}

	public void handleEvent(BasicAgentDepartureEvent event) {
		EventAgent e = new EventAgent();
//		e.starttime = event.getTime();
		e.starttime = 3 * 3600;
		agents.put(event.getPersonId().toString(), e);
		
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void handleEvent(BasicAgentArrivalEvent event) {
		EventAgent e = this.agents.get(event.getPersonId().toString());
		e.endtime = event.getTime();
		
	}
	

	private static class EventAgent {
		double starttime;
		double endtime;
		
	}


	public void handleEvent(BasicAgentStuckEvent event) {
		EventAgent e = this.agents.get(event.getPersonId().toString());
		e.endtime = 30 * 3600;
		
	}

}
