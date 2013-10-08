/* *********************************************************************** *
 * project: org.matsim.*
 * CongestionPerLinkHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.modular;


/**
 * @author benjamin
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

public class LinkLeaveCountsHandler implements LinkLeaveEventHandler {
	private static final Logger logger = Logger.getLogger(LinkLeaveCountsHandler.class);

	Map<Id, ArrayList<LinkLeaveEvent>> linkId2LinkLeaveEvent = new HashMap<Id, ArrayList<LinkLeaveEvent>>();

	public LinkLeaveCountsHandler() {
	}

	@Override
	public void reset(final int iteration) {
		this.linkId2LinkLeaveEvent.clear();
		logger.info("resetting linkId2LinkLeaveEvent to " + this.linkId2LinkLeaveEvent);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		
		Id linkId = event.getLinkId();
		ArrayList<LinkLeaveEvent> events;
		if(this.linkId2LinkLeaveEvent.get(linkId) == null){
			events = new ArrayList<LinkLeaveEvent>();
		} else {
			events = this.linkId2LinkLeaveEvent.get(linkId);
		}
		events.add(event);
		this.linkId2LinkLeaveEvent.put(linkId, events);
	}
	
	public Map<Id, ArrayList<LinkLeaveEvent>> getLinkId2LinkLeaveEvents() {
		return this.linkId2LinkLeaveEvent;
	}
}