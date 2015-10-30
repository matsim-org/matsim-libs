/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.teach.events;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
/**
 * @author jbischoff
 * A basic event handler used in the MATSim class at TU Berlin.
 */
public class MyEventHandler implements ActivityEndEventHandler,
		ActivityStartEventHandler {
	
	private Map<Id<Person>,Double> startTimes = new HashMap<>();
	private Id<Person> personWithHighestWorkDuration;
	private double highestWorkDuration = 0;
		
	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("work")){
			this.startTimes.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("work")){
			double workingTime = event.getTime() - this.startTimes.get(event.getPersonId());
			if (workingTime> this.highestWorkDuration)
			{
				this.highestWorkDuration = workingTime;
				this.personWithHighestWorkDuration = event.getPersonId();
			}
		}
	}
	
	public void printPersonWithHighestWorkingTime(){
		System.out.println(this.personWithHighestWorkDuration.toString() + ": "+this.highestWorkDuration);
	}

}