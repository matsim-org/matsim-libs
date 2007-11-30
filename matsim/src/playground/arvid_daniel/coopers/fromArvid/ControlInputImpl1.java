/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputImpl1.java
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

package playground.arvid_daniel.coopers.fromArvid;

import java.io.IOException;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.withinday.trafficmanagement.AbstractControlInputImpl;
import org.matsim.withinday.trafficmanagement.ControlInput;

/**
 * @author a.bergsten, d.zetterberg
 *
 */

/*

 * Measures the travel time difference between route 1 and 2 and returns that
 * as the control signal. ("Reactive control")
*/

public class ControlInputImpl1 extends AbstractControlInputImpl 
		implements EventHandlerLinkLeaveI, EventHandlerLinkEnterI,
		EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, ControlInput {

	private ControlInputWriter writer;
	
	
	public ControlInputImpl1() {
		this.writer = new ControlInputWriter();
	}

	@Override
	public void init() {
		super.init();
		this.writer.open();

	}
	
	@Override
	public void handleEvent(final EventLinkEnter event) {
		super.handleEvent(event);
	}

		@Override
	public void handleEvent(final EventLinkLeave event) {
		super.handleEvent(event);
		}
		
  
		public double getNashTime() {

			try {
				this.writer.writeTravelTimesMainRoute(this.lastTime1,
						this.lastTime1);
				this.writer.writeTravelTimesAlternativeRoute(this.lastTime2,
						this.lastTime2);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return this.timeDifference;
		}

	public void reset(final int iteration) {
		this.writer.close();
	}

	@Override
	public void handleEvent(final EventAgentDeparture event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final EventAgentArrival event) {
		super.handleEvent(event);
	}

}
