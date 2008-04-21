/* *********************************************************************** *
 * project: org.matsim.*
 * ArrivalTimeCollector.java
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

package teach.multiagent07.simulation;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;

import teach.multiagent07.interfaces.EventHandlerI;
import teach.multiagent07.util.Event;

public class ArrivalTimeCollector implements EventHandlerI{

	Map<Id, Double> agentArrival = new TreeMap<Id, Double>();

	public void handleEvent(Event event) {
		if(event.type == Event.ACT_ARRIVAL && event.legNumber == 1) {
			agentArrival.put(event.agentId, new Double(event.time));
		}
	}

	public double getArrivalTimeAtFirstActivity(Id agentID) {
		double result = 0;

		if(agentArrival.containsKey(agentID))return agentArrival.get(agentID);
		return result;
	}

}
