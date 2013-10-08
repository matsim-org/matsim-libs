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

package playground.wrashid.parkingSearch.withindayFW.parkingTracker;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;

import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;

/**
 * If we have car1-park1-walk1-act-walk2-park2-car2, this class needs to capture
 * the activity start time of park1.
 * 
 * @author wrashid
 * 
 */
public class UpdatePreviousParkingArrivalTime implements ActivityStartEventHandler, ActivityEndEventHandler {

	private ParkingAgentsTracker parkingAgentsTracker;

	public UpdatePreviousParkingArrivalTime(ParkingAgentsTracker parkingAgentsTracker) {
		this.parkingAgentsTracker = parkingAgentsTracker;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// TODO Auto-generated method stub

	}

}
