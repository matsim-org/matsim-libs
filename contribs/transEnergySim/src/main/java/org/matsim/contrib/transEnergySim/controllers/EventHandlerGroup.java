/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.transEnergySim.controllers;

import java.util.LinkedList;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.core.events.handler.EventHandler;

public class EventHandlerGroup implements ActivityStartEventHandler, PersonArrivalEventHandler,
PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, Wait2LinkEventHandler, ActivityEndEventHandler {
	/**
	 * All event handlers needed for electric vehicle simulation. Convenience class.
	 * 
	 */
	protected LinkedList<EventHandler> handler = new LinkedList<EventHandler>();

	public void addHandler(EventHandler handler) {
		this.handler.add(handler);
	}

	@Override
	public void reset(int iteration) {
		for (EventHandler h : handler) {
			h.reset(iteration);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof LinkLeaveEventHandler) {
				((LinkLeaveEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof LinkEnterEventHandler) {
				((LinkEnterEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof PersonDepartureEventHandler) {
				((PersonDepartureEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof PersonArrivalEventHandler) {
				((PersonArrivalEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof ActivityStartEventHandler) {
				((ActivityStartEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof PersonLeavesVehicleEventHandler) {
				((PersonLeavesVehicleEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof PersonEntersVehicleEventHandler) {
				((PersonEntersVehicleEventHandler) h).handleEvent(event);
			}
		}
		
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof Wait2LinkEventHandler) {
				((Wait2LinkEventHandler) h).handleEvent(event);
			}
		}		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof ActivityEndEventHandler) {
				((ActivityEndEventHandler) h).handleEvent(event);
			}
		}	
	}


}
