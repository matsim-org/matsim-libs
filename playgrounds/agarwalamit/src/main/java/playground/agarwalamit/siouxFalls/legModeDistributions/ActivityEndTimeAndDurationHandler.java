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
package playground.agarwalamit.siouxFalls.legModeDistributions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.config.Config;

/**
 * @author amit
 * 
 * need to be tested first.
 */
public class ActivityEndTimeAndDurationHandler implements PersonDepartureEventHandler, ActivityEndEventHandler, ActivityStartEventHandler {

	private final Logger logger = Logger.getLogger(ActivityEndTimeAndDurationHandler.class);
	private Map<String, Map<Id, Double>> activity2personId2StartTime;
	private Map<String, Map<Id, Double>> activity2personId2EndTime;
	private Map<String, Map<Id, Double>> activity2personId2Duration;
	private Map<Id, String> personId2LegMode;

	public ActivityEndTimeAndDurationHandler() {
		this.personId2LegMode = new HashMap<Id, String>();
		this.activity2personId2StartTime = new HashMap<String, Map<Id, Double>>();
		this.activity2personId2EndTime = new HashMap<String, Map<Id, Double>>();
		this.activity2personId2Duration = new HashMap<String, Map<Id,Double>>();
		Config config = new Config();
	}

	@Override
	public void reset(int iteration) {
		this.personId2LegMode.clear();
		this.activity2personId2StartTime.clear();
		this.activity2personId2EndTime.clear();
		this.activity2personId2Duration.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO [AA] what if different departures have different leg mode.
		this.personId2LegMode.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		double activityStartTime =event.getTime();
		String actType = event.getActType();

		if(this.activity2personId2StartTime.containsKey(actType)){
			Map<Id, Double> personId2ActStartTime = this.activity2personId2StartTime.get(actType);
			if(personId2ActStartTime.containsKey(personId)){
				throw new RuntimeException("Person have already started this activity. Can not start again.");
			} else {
				personId2ActStartTime.put(personId, activityStartTime);
			}
		} else {
			Map<Id, Double> personId2ActStartTime = new HashMap<Id, Double>();
			personId2ActStartTime.put(personId, activityStartTime);
			this.activity2personId2StartTime.put(actType, personId2ActStartTime);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		double activityEndTime =event.getTime();
		String actType = event.getActType();

		if(this.activity2personId2EndTime.containsKey(actType)){
			Map<Id, Double> personId2ActEndTime = this.activity2personId2EndTime.get(actType);
			if(personId2ActEndTime.containsKey(personId)){
				throw new RuntimeException("Person "+personId.toString()+" have already ended "+actType+" activity. Can not end again.");
			} else {
				personId2ActEndTime.put(personId, activityEndTime);
			}
		} else {
			Map<Id, Double> personId2ActEndTime = new HashMap<Id, Double>();
			personId2ActEndTime.put(personId, activityEndTime);
			this.activity2personId2EndTime.put(actType, personId2ActEndTime);
		}
	}

	public Map<String, Map<Id, Double>> getActivityType2PersonId2ActStartTime(){
		return this.activity2personId2StartTime;
	}
	public Map<String, Map<Id, Double>> getActivityType2PersonId2ActEndTime(){
		return this.activity2personId2EndTime;
	}

	public Map<Id, String> getPersonId2LegMode(){
		return this.personId2LegMode;
	}

	public Map<String, Map<Id, Double>> getActivityType2PersonId2ActDuration(){
		for(String actType : this.activity2personId2StartTime.keySet()){
			Map<Id, Double> personId2ActDuration = new HashMap<Id, Double>();
			for (Id personId : this.activity2personId2StartTime.get(actType).keySet()){

				if(this.activity2personId2EndTime.get(actType).containsKey(personId)){
					double actDuration = 0;
					actDuration = Math.abs(this.activity2personId2StartTime.get(actType).get(personId)-this.activity2personId2EndTime.get(actType).get(personId));
					personId2ActDuration.put(personId, Double.valueOf(actDuration));
				} else {
					throw new RuntimeException("Person "+personId+" has started activity "+actType+" but not ended.");
				}
			}
			this.activity2personId2Duration.put(actType, personId2ActDuration);
		}

		return this.activity2personId2Duration;
	}
}
