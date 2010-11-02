/* *********************************************************************** *
 * project: org.matsim.*
 * DgSensorManager
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
package playground.dgrether.signalsystems;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;


/**
 * @author dgrether
 *
 */
public class DgSensorManager implements LinkEnterEventHandler, LinkLeaveEventHandler{

	private static final Logger log = Logger.getLogger(DgSensorManager.class);
	
	private Set<Id> monitoredLinkIds = new HashSet<Id>();
	
	public DgSensorManager(){}
	
	public void registerNumberOfCarsMonitoring(Id linkId){
		
	}
	
	public void registerNumberOfCarsInDistanceMonitoring(Id linkId, Double distanceMeter){
		
	}
	

	public void getNumberOfCarsOnLink(Id linkId){
		
	}
	
	public void getNumberOfCarsInDistance(Id linkId, Double distanceMeter){
		
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.monitoredLinkIds.contains(event.getLinkId())){
			
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.monitoredLinkIds.contains(event.getLinkId())){
			
		}
	}
	@Override
	public void reset(int iteration) {
		
	}
	
}
