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

package playground.balmermi.datapuls.modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;

public class FrequencyAnalyser implements LinkLeaveEventHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(FrequencyAnalyser.class);
	private final Map<Id<Link>,Set<Id<Vehicle>>> freqs;
	private long entryCnt = 0;
	private long hour = -1;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	
	public FrequencyAnalyser(final Network network) {
		this(network,network.getLinks().keySet());
	}

	public FrequencyAnalyser(final Network network, Set<Id<Link>> linkIdSet) {
		log.info("init " + this.getClass().getName() + " module...");
		if (linkIdSet == null) { throw new NullPointerException("linkIdSet cannot be null"); }
		freqs = new HashMap<>((int)(network.getLinks().size()*1.4));
		for (Id<Link> lid : linkIdSet) { freqs.put(lid, new HashSet<Id<Vehicle>>()); }
		log.info("=> "+freqs.size()+" sets allocated.");
		log.info("done. (init)");
	}

	//////////////////////////////////////////////////////////////////////
	// interface methods
	//////////////////////////////////////////////////////////////////////
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Set<Id<Vehicle>> pids = freqs.get(event.getLinkId());
		if (pids == null) { return; }
		if (pids.add(event.getVehicleId())) { entryCnt++; }
		// logging info
		if ((entryCnt != 0) && (entryCnt % freqs.size() == 0)) {
			log.info(entryCnt+" entries added to the frequency map.");
			Gbl.printMemoryUsage();
		}
		if (((int)(event.getTime()/3600.0)) != hour) { hour++; log.info("at hour "+hour); }
	}

	@Override
	public void reset(int iteration) {
		log.info("reset " + this.getClass().getName() + " module...");
		for (Set<Id<Vehicle>> idSet : freqs.values()) { idSet.clear(); }
		entryCnt = 0;
		hour = -1;
		log.info("done. (reset)");
	}
	
	public void resetLog() {
		log.info("reset log of " + this.getClass().getName() + " module...");
		entryCnt = 0;
		hour = -1;
		log.info("done. (reset log)");
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////
	
	public final Map<Id<Link>,Set<Id<Vehicle>>> getFrequencies() {
		return freqs;
	}
}
