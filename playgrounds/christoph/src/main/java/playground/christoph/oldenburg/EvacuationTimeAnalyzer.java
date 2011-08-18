/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTimeAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.oldenburg;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;

public class EvacuationTimeAnalyzer implements AgentArrivalEventHandler {

	/*package*/ double longestEvacuationTime = 0.0;
	/*package*/ double sumEvacuationTimes = 0.0;
	
	@Override
	public void reset(int iteration) {
		longestEvacuationTime = 0.0;
		sumEvacuationTimes = 0.0;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		double evacuationTime = event.getTime() - DemoConfig.evacuationTime;
		
		longestEvacuationTime = evacuationTime;
		sumEvacuationTimes += evacuationTime;
	}
}
