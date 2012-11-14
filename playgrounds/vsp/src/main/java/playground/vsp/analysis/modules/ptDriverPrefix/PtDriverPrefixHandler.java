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
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

/**
 * Collects the public vehicle Driver IDs.
 * 
 * @author ikaddoura
 *
 */
public class PtDriverPrefixHandler implements TransitDriverStartsEventHandler {
	private final static Logger log = Logger.getLogger(PtDriverPrefixHandler.class);
	private List<Id> ptDriverIDs = new ArrayList<Id>();
	private String ptDriverPrefix = null;

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
		return ptDriverIDs;
	}

	public String getPtDriverPrefix() {
		List<Id> ptDriverIds = this.ptDriverIDs;
		
		if (ptDriverIds.isEmpty()){
			log.warn("No pt driver(s) identified so far. Thus the ptDriverPrefix could not be set.");
		} else {
			this.ptDriverPrefix = ptDriverIds.get(0).toString().substring(0, 3); // first three letters
			for (Id ptDriverId : ptDriverIds){
				String prefix = ptDriverId.toString().substring(0, 3);
				if(!prefix.equals(this.ptDriverPrefix)) {
					throw new RuntimeException("Can't deal with more than one pt driver prefix. (Found prefixes: " + prefix + " and " + this.ptDriverPrefix + ") Aborting...");
				}
			}
		}
		return this.ptDriverPrefix;
	}
	
}
