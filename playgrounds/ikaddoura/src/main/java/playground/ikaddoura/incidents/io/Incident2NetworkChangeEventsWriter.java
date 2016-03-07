/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.io;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.ikaddoura.incidents.DateTime;
import playground.ikaddoura.incidents.TMCAlerts;
import playground.ikaddoura.incidents.data.TrafficItem;

/**
* @author ikaddoura
*/

public class Incident2NetworkChangeEventsWriter {

	private Map<String, TrafficItem> trafficItems = null;
	private Map<String, Path> trafficItemId2path = null;
	private TMCAlerts tmc = null;
	private NetworkChangeEventFactory nceFactory;
	
	public Incident2NetworkChangeEventsWriter(TMCAlerts tmc, Map<String, TrafficItem> trafficItems, Map<String, Path> trafficItemId2path) {
		this.tmc = tmc;
		this.trafficItems = trafficItems;
		this.trafficItemId2path = trafficItemId2path;
		
		nceFactory = new NetworkChangeEventFactoryImpl();

	}

	public void writeIncidentLinksToNetworkChangeEventFile(String startDateTime, String endDateTime) {
		
		double startDate = DateTime.parseDateTimeToSeconds(startDateTime);
		double endDate = DateTime.parseDateTimeToSeconds(endDateTime);
		
		double dateInSec = startDate;
		
		while (dateInSec <= endDate) {
			
			for (TrafficItem item : this.trafficItems.values()) {
				
				if (DateTime.parseDateTimeToSeconds(item.getEndTime()) < dateInSec 
						|| DateTime.parseDateTimeToSeconds(item.getStartTime()) > dateInSec + (24 * 3600.)) {
					// the traffic item is not relevant for this day
				} else {
					// TODO
				}
				
			}
			
			dateInSec = dateInSec + 24 * 3600.;
		}	
	}

}

