/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureEventHandler.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.version4;

import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

/**
 * @author Ihab
 *
 */
public class DepartureEventHandler implements AgentDepartureEventHandler {
	private int numberOfPtLegs;
	private int numberOfCarLegs;
	private int numberOfWalkLegs; // Walk & TransitWalk
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(event.getLegMode().toString().equals("pt")){
			this.numberOfPtLegs++;
		}
		if(event.getLegMode().toString().equals("car")){
			if (!event.getPersonId().toString().contains("bus")){
				this.numberOfCarLegs++;
			}
			else {
				// The DepartureEvent is caused by a bus!
			}
		}
		if(event.getLegMode().toString().equals("walk") || event.getLegMode().toString().equals("transit_walk")){
			this.numberOfWalkLegs++;
		}
	}

	public int getNumberOfPtLegs() {
		return this.numberOfPtLegs;
	}

	public int getNumberOfCarLegs() {
		return this.numberOfCarLegs;
	}
	
	public int getNumberOfWalkLegs() {
		return this.numberOfWalkLegs;
	}

}
