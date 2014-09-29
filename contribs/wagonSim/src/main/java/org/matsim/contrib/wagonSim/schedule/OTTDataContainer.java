/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.wagonSim.schedule;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author balmermi @ Seonzon AG
 * @since 3013-07-08
 */
public class OTTDataContainer {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	final Map<Id<Locomotive>,Locomotive> locomotives = new HashMap<>();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public OTTDataContainer() {
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void print() {
		for (Locomotive locomotive : locomotives.values()) { System.out.println(locomotive.toString()); }
	}
	
	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////

	static class Locomotive {
		final Id<Locomotive> id;
		final Integer type;
		SortedMap<Date,StationData> trips = new TreeMap<Date,StationData>();
		
		Locomotive(int locNr, int locType) { id = Id.create(locNr, Locomotive.class); type = locType; }
		
		@Override
		public final String toString() {
			String str = "";
			for (StationData stationData : trips.values()) { str += id.toString()+";"+stationData.toString()+";"+type+"\n"; }
			return str;
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	static class StationData {
		Date arrival = null;
		Date departure = null;
		Double delayArrival = null;
		Double delayDeparture = null;
		Id<TransitStopFacility> stationId = null;
		
		@Override
		public final String toString() { return arrival.toString()+";"+departure.toString()+";"+delayArrival+";"+delayDeparture+";"+stationId.toString(); }
	}
}
