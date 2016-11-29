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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import playground.jbischoff.taxibus.scenario.analysis.WobDistanceAnalyzer;

/**
 * @author  jbischoff
 *
 */
public class TTLocationBasedEventHandler implements ActivityStartEventHandler, PersonDepartureEventHandler, ActivityEndEventHandler, LinkEnterEventHandler
{
	Map<Id<Person>, String> lastActivity = new HashMap<>();
	Map<Id<Person>, Double> lastDeparture = new HashMap<>();
	
	Map<String,Double> ttToActivity = new TreeMap<>();
	Map<String,Integer> legsToActivity = new HashMap<>();
	private Set<Id<Link>> monitoredLinks;
	private Set<Id<Person>> monitoredAgents = new HashSet<>();
	

	public TTLocationBasedEventHandler(Set<Id<Link>> monitoredLinks ) {
		this.monitoredLinks = monitoredLinks;
	}
	
	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!event.getLegMode().equals("car")) return;
		if (!isRelevantPerson(event.getPersonId())) return;

		this.lastDeparture.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().startsWith("pt")) return;
		if (!isRelevantPerson(event.getPersonId())) return;
		if (lastDeparture.containsKey(event.getPersonId())){
		double departureTime = this.lastDeparture.remove(event.getPersonId());
		double travelTime = event.getTime()-departureTime;
		String as = buildActivityString(this.lastActivity.get(event.getPersonId()), event.getActType());
		if (this.monitoredAgents.contains(event.getPersonId())){
		addTTtoActivity(as, travelTime);
		}
		}
//		else {
//			System.err.println(event.getPersonId() + " at act "+event.getActType() +" had no departure");
//		}
	}
	boolean isRelevantPerson(Id<Person> personId){
		return (personId.toString().endsWith("vw")?true:false);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().startsWith("pt")) return;
		if (!isRelevantPerson(event.getPersonId())) return;

		this.lastActivity.put(event.getPersonId(), event.getActType());

	}
	
	private void addTTtoActivity(String activityString, double traveltime){
		double tt = traveltime;
		int legs = 1;
		if (this.ttToActivity.containsKey(activityString)){
			traveltime += this.ttToActivity.get(activityString);
			legs += this.legsToActivity.get(activityString);
		}
		this.legsToActivity.put(activityString, legs);
		this.ttToActivity.put(activityString, traveltime);
	}
	
	
	
	private String buildActivityString(String fromAct, String toAct){
		return fromAct+"--"+toAct;
	}
	
	
	public void printOutput(){
		System.out.println("tt between Activities for Agents that pass through links "+this.monitoredLinks.toString());
		System.out.println("Activity\tLegs\tAveTT");
		for (Entry<String,Double> e : this.ttToActivity.entrySet()){
			double legs = this.legsToActivity.get(e.getKey());
			System.out.println(e.getKey()+"\t"+legs+"\t"+ WobDistanceAnalyzer.prettyPrintSeconds(e.getValue()/legs));
		}

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.monitoredLinks.contains(event.getLinkId())){
			Id<Person> personId = Id.createPersonId(event.getVehicleId().toString());
			this.monitoredAgents.add(personId);
		}
	}
}
