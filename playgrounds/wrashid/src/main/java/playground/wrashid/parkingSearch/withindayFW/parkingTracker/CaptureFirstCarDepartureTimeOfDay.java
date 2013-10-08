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

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

public class CaptureFirstCarDepartureTimeOfDay implements PersonDepartureEventHandler {

	HashMap<Id, Double> firstCarDepartureTime;
	
	public CaptureFirstCarDepartureTimeOfDay(){
		firstCarDepartureTime=new HashMap<Id, Double>();
	}
	
	public Double getTime(Id agentId){
		return firstCarDepartureTime.get(agentId);
	}
	
	@Override
	public void reset(int iteration) {
		firstCarDepartureTime.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id personId = event.getPersonId();
		if (!firstCarDepartureTime.containsKey(personId)){
			firstCarDepartureTime.put(personId, event.getTime());
		}
	}

}
