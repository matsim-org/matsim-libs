/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;

/**
 * @author tthunig
 */
public class TtStaticLinkFlowValues implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler{

	private Map<Id<Link>, Integer> staticLinkFlows = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		staticLinkFlows.clear();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		increment(event.getLinkId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		increment(event.getLinkId());
	}
	
	private void increment(Id<Link> linkId) {
		if (!staticLinkFlows.containsKey(linkId))
			staticLinkFlows.put(linkId, 0);
		int previousEntry = staticLinkFlows.get(linkId);
		staticLinkFlows.put(linkId, previousEntry + 1);
	}

	public Map<Id<Link>, Integer> getStaticLinkFlows() {
		return staticLinkFlows;
	}
	
	public int getStaticLinkFlow(Id<Link> linkId) {
		if (!staticLinkFlows.containsKey(linkId))
			return 0;
		return staticLinkFlows.get(linkId);
	}

}
