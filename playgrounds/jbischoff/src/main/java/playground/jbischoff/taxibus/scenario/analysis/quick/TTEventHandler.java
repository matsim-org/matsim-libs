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

package playground.jbischoff.taxibus.scenario.analysis.quick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.jbischoff.taxibus.scenario.analysis.WobDistanceAnalyzer;

/**
 * @author jbischoff
 *
 */
public class TTEventHandler implements ActivityStartEventHandler, PersonDepartureEventHandler, ActivityEndEventHandler {
	Map<Id<Person>, String> lastActivity = new HashMap<>();
	Map<Id<Person>, Double> lastDeparture = new HashMap<>();
	Map<String, Double> ttToActivity = new TreeMap<>();
	Map<String, Integer> legsToActivity = new HashMap<>();
	ArrayList<String> monitoredModes = new ArrayList<>(Arrays.asList(new String[] { "car"}));
	

	public TTEventHandler() {

	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!this.monitoredModes.contains(event.getLegMode()))
			return;
		if (!isRelevantPerson(event.getPersonId()))
			return;

		this.lastDeparture.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().startsWith("pt"))
			return;
		if (!isRelevantPerson(event.getPersonId()))
			return;
		if (lastDeparture.containsKey(event.getPersonId())) {
			double departureTime = this.lastDeparture.remove(event.getPersonId());
			double travelTime = event.getTime() - departureTime;
			String as = buildActivityString(this.lastActivity.get(event.getPersonId()), event.getActType());
			addTTtoActivity(as, travelTime);
		}
		// else {
		// System.err.println(event.getPersonId() + " at act
		// "+event.getActType() +" had no departure");
		// }
	}

	boolean isRelevantPerson(Id<Person> personId) {
//		return (personId.toString().endsWith("vw") ? true : false);
		return (personId.toString().startsWith("BS_WB") ? true : false);
//		return true;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().startsWith("pt"))
			return;
		if (!isRelevantPerson(event.getPersonId()))
			return;

		this.lastActivity.put(event.getPersonId(), event.getActType());

	}

	private void addTTtoActivity(String activityString, double traveltime) {
		double tt = traveltime;
		int legs = 1;
		if (this.ttToActivity.containsKey(activityString)) {
			traveltime += this.ttToActivity.get(activityString);
			legs += this.legsToActivity.get(activityString);
		}
		this.legsToActivity.put(activityString, legs);
		this.ttToActivity.put(activityString, traveltime);
	}

	private String buildActivityString(String fromAct, String toAct) {
		return fromAct + "--" + toAct;
	}

	public void printOutput() {
		System.out.println("tt between Activities");
		System.out.println("Activity\tLegs\tAveTT");
		for (Entry<String, Double> e : this.ttToActivity.entrySet()) {
			double legs = this.legsToActivity.get(e.getKey());
			System.out.println(
					e.getKey() + "\t" + legs + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(e.getValue() / legs));
		}

	}
}
