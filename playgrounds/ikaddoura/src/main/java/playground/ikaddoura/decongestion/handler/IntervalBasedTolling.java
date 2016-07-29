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

package playground.ikaddoura.decongestion.handler;

import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.ikaddoura.decongestion.data.DecongestionInfo;

/**
 * Throws agent money events for the tolled links and time bins.
 * 
 * @author ikaddoura
 */

public class IntervalBasedTolling implements LinkLeaveEventHandler {

	private final EventsManager eventsManager;
	private final DecongestionInfo decongestionInfo;
	private double totalTollPayments;

	public IntervalBasedTolling(DecongestionInfo congestionInfo, EventsManager events) {
		this.decongestionInfo = congestionInfo;
		this.eventsManager = events;
	}

	@Override
	public void reset(int iteration) {
		this.totalTollPayments = 0.;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (decongestionInfo.getlinkInfos().containsKey(event.getLinkId())) {
			
			int currentTimeBin = (int) (event.getTime() / this.decongestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());
									
			if (decongestionInfo.getlinkInfos().get(event.getLinkId()).getTime2toll().containsKey(currentTimeBin)) {
				double toll = decongestionInfo.getlinkInfos().get(event.getLinkId()).getTime2toll().get(currentTimeBin);
				this.eventsManager.processEvent(new PersonMoneyEvent(event.getTime(), this.decongestionInfo.getVehicleId2personId().get(event.getVehicleId()), -1. * toll));
				this.totalTollPayments = this.totalTollPayments + toll;
			}		
		}
	}

	public double getTotalTollPayments() {
		return totalTollPayments;
	}

}

