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
package playground.agarwalamit.analysis.activity;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */
public class ActivityDurationHandler implements ActivityEndEventHandler, ActivityStartEventHandler {

	/**
	 * @param simStartTime simulation start time
	 * @param simEndTime simulation end time
	 */
	public ActivityDurationHandler(double simStartTime, double simEndTime) {
		this.simStartTime = simStartTime;
		this.simEndTime = simEndTime;
	}

	/**
	 * default simulation start and end time are set to 00:00:00 and 30:00:00
	 */
	public ActivityDurationHandler() {
		this.simStartTime = 0;
		this.simEndTime = 30*3600;
	}

	private final double simStartTime;
	private final double simEndTime;
	
	private Map<Id<Person>, Double> person2TotalActDuration = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.person2TotalActDuration.clear();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		double actDurSoFar = person2TotalActDuration.get(event.getPersonId());
		double actDur = actDurSoFar-event.getTime();
		person2TotalActDuration.put(event.getPersonId(), actDur);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(person2TotalActDuration.containsKey(event.getPersonId())){
			double actDurSoFar = person2TotalActDuration.get(event.getPersonId());
			double actDur = actDurSoFar+event.getTime();
			person2TotalActDuration.put(event.getPersonId(), actDur);
		} else {
			double actDur = simStartTime+simEndTime;
			person2TotalActDuration.put(event.getPersonId(), actDur);
		}
	}

	/**
	 * @return sum of all activity durations of all persons.
	 */
	public double getTotalActivityDuration (){
		double sum =0;
		for(Id<Person> id :person2TotalActDuration.keySet()){
			sum += person2TotalActDuration.get(id);
		}
		return sum;
	}

	/**
	 * @return sum of all activity durations of each person.
	 */
	public Map<Id<Person>, Double> getPersonId2TotalActDuration(){
		return person2TotalActDuration;
	}

}
