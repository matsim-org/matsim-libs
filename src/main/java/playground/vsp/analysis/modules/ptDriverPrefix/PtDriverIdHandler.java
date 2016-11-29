/* *********************************************************************** *
 * project: org.matsim.*
 * TransitEventHandler.java
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
package playground.vsp.analysis.modules.ptDriverPrefix;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;

/**
 * Collects the public vehicle Driver IDs.
 * 
 * @author ikaddoura
 *
 */
public class PtDriverIdHandler implements TransitDriverStartsEventHandler {
	private final static Logger log = Logger.getLogger(PtDriverIdHandler.class);
	private final static List<Id> ptDriverIDs = new ArrayList<Id>(); //was neither final not static

	@Override
	public void reset(int iteration) {
		this.ptDriverIDs.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		Id ptDriverId = event.getDriverId();
		
		if (!this.ptDriverIDs.contains(ptDriverId)){
			this.ptDriverIDs.add(ptDriverId);
			
		}
	}

	public List<Id> getPtDriverIDs() {
		
		if (ptDriverIDs.isEmpty()){
			log.warn("No pt driver(s) identified. List is empty!");
		}
		return ptDriverIDs;
	}
	
}
