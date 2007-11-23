/* *********************************************************************** *
 * project: org.matsim.*
 * TimeWriter.java
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

package playground.yu;

import java.util.HashMap;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.algorithms.EventWriterTXT;

/**
 * prepare Departure time- arrival Time- Diagramm
 * @author ychen
 * 
 */
public class TimeWriter extends EventWriterTXT {
	private HashMap<String, Double> agentDepTimes;

	public TimeWriter(final String filename) {
		super(filename);
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.demandmodeling.events.algorithms.EventWriterTXT#handleEvent(org.matsim.demandmodeling.events.BasicEvent)
	 */
	@Override
	public void handleEvent(BasicEvent event) {
		String agentId = event.agentId;
		if (event instanceof EventAgentDeparture) {
//			EventAgentDeparture ead = (EventAgentDeparture) event;
			if (Integer.parseInt(event.getAttributes().getValue("leg")) == 0) {
				agentDepTimes.put(agentId, event.time);
			}
		} else if (event instanceof EventAgentArrival
				&& agentDepTimes.containsKey(agentId)) {
			int depT=(int)agentDepTimes.remove(agentId).doubleValue();
			int depH=depT/3600;
			int depMin=(depT-depH*3600)/60;
			int depSec=depT-depH*3600-depMin*60;
			int time=(int)event.time;
			int h=time/3600;
			int min=(time-h*3600)/60;
			int sec=time-h*3600-min*60;
			writeLine(agentId + "\t" + depH+":"+depMin+":"+depSec + "\t"+ h+":"+min+":"+sec);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.demandmodeling.events.algorithms.EventWriterTXT#init(java.lang.String)
	 */
	@Override
	public void init(String outfilename) {
		super.init(outfilename);
		writeLine("agentId\tdepTime\tarrTime");
		agentDepTimes = new HashMap<String, Double>();
	}

}
