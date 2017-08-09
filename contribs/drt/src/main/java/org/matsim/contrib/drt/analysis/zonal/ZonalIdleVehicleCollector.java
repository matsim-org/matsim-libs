/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.analysis.zonal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ZonalIdleVehicleCollector implements ActivityStartEventHandler, ActivityEndEventHandler {

	private Map<String,MutableInt> vehiclesPerZone = new HashMap<>();
	private final DrtZonalSystem zonalSystem; 
	
	/**
	 * 
	 */
	@Inject
	public ZonalIdleVehicleCollector(EventsManager events, DrtZonalSystem zonalSystem) {
		events.addHandler(this);
		this.zonalSystem = zonalSystem;
		for (String z : zonalSystem.getZones().keySet()){
			vehiclesPerZone.put(z, new MutableInt());
		}
	}
	
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)){
			String zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone!=null){
				vehiclesPerZone.get(zone).increment();
			}
		}
		
	}



	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)){
			String zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone!=null){
				vehiclesPerZone.get(zone).decrement();
			}
		}	
	}

}
