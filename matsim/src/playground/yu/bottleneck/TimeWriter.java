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

package playground.yu.bottleneck;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.utils.io.IOUtils;

/**
 * prepare Departure time- arrival Time- Diagramm
 * @author ychen
 */
public class TimeWriter implements EventHandlerAgentDepartureI, EventHandlerAgentArrivalI {
//	-------------------------MEMBER VARIABLES---------------------------------
	private BufferedWriter out = null;
	private HashMap<String, Double> agentDepTimes;
//--------------------------CONSTRUCTOR-------------------------------------
	public TimeWriter(final String filename) {
		init(filename);
	}

	/**
	 * If an agent departures, will the information be saved in a hashmap (agentDepTimes). 
	 */
	public void handleEvent(final EventAgentDeparture event) {
		if (event.legId == 0) {
			this.agentDepTimes.put(event.agentId, event.time);
		}
	}

	/**
	 * If an agent arrives, will the "agent-ID", "depTime" and "arrTime" be written in a .txt-file
	 */
	public void handleEvent(final EventAgentArrival event) {
		String agentId = event.agentId;
		if (this.agentDepTimes.containsKey(agentId)) {
			int depT=(int)this.agentDepTimes.remove(agentId).doubleValue();
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

	public void init(final String outfilename) {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			this.out = IOUtils.getBufferedWriter(outfilename);
			writeLine("agentId\tdepTime\tarrTime");
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.agentDepTimes = new HashMap<String, Double>();
	}

	private void writeLine(final String line) {
		try {
			this.out.write(line);
			this.out.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reset(final int iteration) {
		this.agentDepTimes.clear();
	}

	public void closefile() {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
