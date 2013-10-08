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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;

public class UpdateEndTimeOfPreviousActivity implements ActivityEndEventHandler {

	private final Map<Id, Double> endTimeOfPreviousActivity;

	public UpdateEndTimeOfPreviousActivity(Map<Id, Double> endTimeOfPreviousActivity) {
		this.endTimeOfPreviousActivity = endTimeOfPreviousActivity;
	}
	
	@Override
	public void reset(int iteration) {
		this.endTimeOfPreviousActivity.clear();		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		endTimeOfPreviousActivity.put(event.getPersonId(), event.getTime());		
	}

}
