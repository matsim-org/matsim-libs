/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */
public class ActivityDurationHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final double simStartTime =0;
	private final double simEndTime = 30*3600;
	private Map<Id<Person>, Double> person2TotalActDuration = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.person2TotalActDuration.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double actDurSoFar = person2TotalActDuration.get(event.getPersonId());
		double actDur = actDurSoFar-event.getTime();
		person2TotalActDuration.put(event.getPersonId(), actDur);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(person2TotalActDuration.containsKey(event.getPersonId())){
			double actDurSoFar = person2TotalActDuration.get(event.getPersonId());
			double actDur = actDurSoFar+event.getTime();
			person2TotalActDuration.put(event.getPersonId(), actDur);
		} else {
			double actDur = simStartTime+simEndTime;
			person2TotalActDuration.put(event.getPersonId(), actDur);
		}
	}

	public double getTotalActivityDuration (){
		double sum =0;
		for(Id<Person> id :person2TotalActDuration.keySet()){
			sum += person2TotalActDuration.get(id);
		}
		return sum;
	}
	
	public Map<Id<Person>, Double> getPersonId2TotalActDuration(){
		return person2TotalActDuration;
	}
	
}
