/* *********************************************************************** *
 * project: org.matsim.*
 * XYDataFilter.java
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

package playground.gregor.sim2d_v3.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * Filters XYVxVyEvents before they are sent to an EventsManager which
 * processes the events and sends them to the EventsHandlers.
 * 
 * @author cdobler
 */
public class XYDataFilter {

	static final Logger log = Logger.getLogger(XYDataFilter.class);
	
	private final double dTolerance = 0.50;	// absolute distance
	private final double vTolerance = 1.25;	// relative speed
	private final double tTolerance = 5.0;	// time
	
	private final Map<Id, XYVxVyEvent> recentEvents = new ConcurrentHashMap<Id, XYVxVyEvent>();
	
	private AtomicLong processedEvents = new AtomicLong(0);
	private AtomicLong skippedEvents = new AtomicLong(0);
	
	public boolean processXYVxVyEvent(XYVxVyEvent event) {
		
		XYVxVyEvent recentEvent = recentEvents.get(event.getPersonId());
		boolean processEvent = checkEvent(event, recentEvent);
		
		if (processEvent) {
			// process event, therefore save it as new recently processed event
			this.recentEvents.put(event.getPersonId(), event);
			processedEvents.incrementAndGet();
		} else {
			skippedEvents.incrementAndGet();			
		}
		
		return processEvent;
	}
	
	private boolean checkEvent(XYVxVyEvent event, XYVxVyEvent recentEvent) {
		if (recentEvent != null) {
			
			// check time tolerance
			if (event.getTime() > recentEvent.getTime() + tTolerance) return true;
			
			// check position tolerance
			double xRecent = recentEvent.getX();
			double yRecent = recentEvent.getY();
			
			if (Math.abs(event.getX() - xRecent) > dTolerance) return true;
			else if (Math.abs(event.getY() - yRecent) > dTolerance) return true;
			
//			// check speed tolerance
//			double vx = event.getVX();
//			double vy = event.getVY();
//			double vxRecent = recentEvent.getVX();
//			double vyRecent = recentEvent.getVY();
//						
//			if (vx > vxRecent * vTolerance) return true;
//			else if (vx < vxRecent / vTolerance) return true;
//			else if (vy > vyRecent * vTolerance) return true;
//			else if (vy < vyRecent / vTolerance) return true;			

			return false;
		} else {
			return true;
		}
	}
	
	public void afterSim() {
		log.info("Processed events: " + processedEvents.get());
		log.info("Skipped events: " + skippedEvents.get());
		
		this.recentEvents.clear();
		this.processedEvents.set(0);
		this.skippedEvents.set(0);
	}
}
