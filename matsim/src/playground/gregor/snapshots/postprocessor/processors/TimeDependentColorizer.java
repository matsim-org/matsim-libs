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

package playground.gregor.snapshots.postprocessor.processors;


import java.util.HashMap;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;




public class TimeDependentColorizer implements PostProcessorI, AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler{

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

	public void handleEvent(AgentDepartureEvent event) {
		EventAgent e = new EventAgent();
		e.starttime = event.time;
		
		agents.put(event.agentId, e);
		
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void handleEvent(AgentArrivalEvent event) {
		EventAgent e = this.agents.get(event.agentId);
		e.endtime = event.time;
		
	}
	

	private static class EventAgent {
		double starttime;
		double endtime;
		
	}


	public void handleEvent(AgentStuckEvent event) {
		EventAgent e = this.agents.get(event.agentId);
		e.endtime = 30 * 3600;
		
	}
}
