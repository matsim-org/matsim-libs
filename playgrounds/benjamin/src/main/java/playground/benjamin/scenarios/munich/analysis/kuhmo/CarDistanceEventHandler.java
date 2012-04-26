/* *********************************************************************** *
 * project: org.matsim.*
 * CarDistanceEventHandler.java
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
package playground.benjamin.scenarios.munich.analysis.kuhmo;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author benjamin
 *
 */
public class CarDistanceEventHandler implements LinkLeaveEventHandler{
	private final static Logger logger = Logger.getLogger(CarDistanceEventHandler.class);

	private Map<Id, Double> personId2CarDistance;
	private final Network network;
	
	public CarDistanceEventHandler(Network network) {
		this.personId2CarDistance = new HashMap<Id, Double>();
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.personId2CarDistance = new HashMap<Id, Double>();
		logger.info("resetting personId2CarDistance to " + this.personId2CarDistance + " ...");
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		Double linkLength_m = this.network.getLinks().get(linkId).getLength();
		
		if(this.personId2CarDistance.get(personId) == null){
			this.personId2CarDistance.put(personId, linkLength_m);
		} else {
			double distanceSoFar = this.personId2CarDistance.get(personId);
			double distanceAfterEvent = distanceSoFar + linkLength_m;
			this.personId2CarDistance.put(personId, distanceAfterEvent);
		}
	}

	public Map<Id, Double> getPersonId2CarDistance() {
		return this.personId2CarDistance;
	}

}
