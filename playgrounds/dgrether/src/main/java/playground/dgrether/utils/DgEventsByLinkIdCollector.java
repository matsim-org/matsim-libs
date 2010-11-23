/* *********************************************************************** *
 * project: org.matsim.*
 * EventsByLinkIdCollector
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

public class DgEventsByLinkIdCollector implements LinkLeaveEventHandler {
	// person -> linkId -> LinkLeaveEvent
	private Map<Id, Map<Id, LinkLeaveEvent>> events = new HashMap<Id, Map<Id, LinkLeaveEvent>>();
	private Set<Id> linkIds;
	
	public DgEventsByLinkIdCollector(Set<Id> linkIds) {
		this.linkIds  = linkIds;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.linkIds.contains(event.getLinkId())){
			Map<Id, LinkLeaveEvent> map = this.events.get(event.getPersonId());
			if (map == null){
				map = new HashMap<Id, LinkLeaveEvent>();
				events.put(event.getPersonId(), map);
			}
			map.put(event.getLinkId(), event);
		}
	}

	public LinkLeaveEvent getLinkLeaveEvent(Id personId, Id linkId){
		Map<Id, LinkLeaveEvent> m = this.events.get(personId);
		if (m == null){
			throw new IllegalArgumentException("Cannot find link leave events for person " + personId);
		}
		return m.get(linkId);
	}
	
	@Override
	public void reset(int iteration) {
		this.events.clear();
	}

}