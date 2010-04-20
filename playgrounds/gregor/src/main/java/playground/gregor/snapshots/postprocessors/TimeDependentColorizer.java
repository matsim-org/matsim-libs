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

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;

import playground.gregor.snapshots.writers.PositionInfo;




public class TimeDependentColorizer implements PostProcessorI, AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler{

//	private final Plans plans;
	private final HashMap<String,EventAgent> agents = new HashMap<String,EventAgent>();
	private final double startTime;
//	public TimeDependentColorizer(Plans plans) {
//		this.plans = plans;
//	}

	public TimeDependentColorizer(double startTime) {
		this.startTime = startTime;
	}

	public String[] processEvent(String[] event){
		String id = event[0];
		EventAgent e = this.agents.get(id);
		int time =  (int) ((e.endtime-e.starttime) / 60);
		event[7] = Integer.toString(Math.min(time,255));
		return event;
	}

	public void processPositionInfo(PositionInfo pos) {
		EventAgent e = this.agents.get(pos.getId().toString());
		int time =  (int) ((e.endtime-e.starttime) / 60);
		pos.setType(Math.min(time,255));
	}

	public void handleEvent(AgentDepartureEvent event) {
		EventAgent e = new EventAgent();
//		e.starttime = event.getTime();
		e.starttime = this.startTime;
		this.agents.put(event.getPersonId().toString(), e);
		
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void handleEvent(AgentArrivalEvent event) {
		EventAgent e = this.agents.get(event.getPersonId().toString());
		e.endtime = event.getTime();
		
	}
	

	private static class EventAgent {
		double starttime;
		double endtime;
		
	}


	public void handleEvent(AgentStuckEvent event) {
		EventAgent e = this.agents.get(event.getPersonId().toString());
		e.endtime = 30 * 3600;
		
	}

}
