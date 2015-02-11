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
package playground.agarwalamit.analysis.congestion;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;


/**
 * @author amit
 */
public class CongestionPerPersonHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
PersonDepartureEventHandler, PersonArrivalEventHandler {
	public final Logger logger = Logger.getLogger(CongestionPerPersonHandler.class);

	private Map<Double, Map<Id<Person>, Double>> personId2DelaysPerTimeBin = new HashMap<Double, Map<Id<Person>, Double>>();
	private Map<Id<Link>, Map<Id<Person>, Double>> linkId2PersonIdLinkEnterTime = new HashMap<Id<Link>, Map<Id<Person>,Double>>();
	private Map<Id<Link>, Double> linkId2FreeSpeedLinkTravelTime = new HashMap<Id<Link>, Double>();
	private Map<Double, Map<Id<Link>, Double>> time2linkIdLeaveCount = new HashMap<Double, Map<Id<Link>,Double>>();
	private double totalDelay;

	private final double timeBinSize;

	/**
	 * @param noOfTimeBins
	 * @param simulationEndTime
	 * @param scenario must have minimally network and plans file.
	 */
	public CongestionPerPersonHandler(int noOfTimeBins, double simulationEndTime, Scenario scenario){

		this.timeBinSize = simulationEndTime / noOfTimeBins;
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			this.linkId2PersonIdLinkEnterTime.put(link.getId(), new HashMap<Id<Person>, Double>());
			Double freeSpeedLinkTravelTime = Double.valueOf(Math.floor(link.getLength()/link.getFreespeed())+1);
			this.linkId2FreeSpeedLinkTravelTime.put(link.getId(), freeSpeedLinkTravelTime);
		}

		for(int i =0;i<noOfTimeBins;i++){
			this.personId2DelaysPerTimeBin.put(this.timeBinSize*(i+1), new HashMap<Id<Person>, Double>());
			this.time2linkIdLeaveCount.put(this.timeBinSize*(i+1), new HashMap<Id<Link>, Double>());
			this.personId2DelaysPerTimeBin.put(this.timeBinSize*(i+1), new HashMap<Id<Person>,Double>());

			for(Person person : scenario.getPopulation().getPersons().values()){
				Map<Id<Person>, Double>	delayForPerson = this.personId2DelaysPerTimeBin.get(this.timeBinSize*(i+1));
				delayForPerson.put(person.getId(), Double.valueOf(0.));
			}
			
			for(Link link : scenario.getNetwork().getLinks().values()) {
				Map<Id<Link>, Double> countOnLink = this.time2linkIdLeaveCount.get(this.timeBinSize*(i+1));
				countOnLink.put(link.getId(), Double.valueOf(0.));
			}
		}
	}

	@Override
	public void reset(int iteration) {
		
		this.personId2DelaysPerTimeBin.clear();
		this.logger.info("Resetting person delays to   " + this.personId2DelaysPerTimeBin);
		this.linkId2PersonIdLinkEnterTime.clear();
		this.linkId2FreeSpeedLinkTravelTime.clear();
		this.time2linkIdLeaveCount.clear();
		this.logger.info("Resetting linkLeave counter to " + this.time2linkIdLeaveCount);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = event.getPersonId();

		Map<Id<Person>, Double> personId2LinkEnterTime = this.linkId2PersonIdLinkEnterTime.get(linkId);
		double derivedLinkEnterTime = event.getTime()+1-this.linkId2FreeSpeedLinkTravelTime.get(linkId);
		personId2LinkEnterTime.put(personId, derivedLinkEnterTime);
		this.linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double time = event.getTime(); 
		if(time ==0.0) time = this.timeBinSize;
		double endOfTimeInterval = 0.0;
		endOfTimeInterval = Math.ceil(time/this.timeBinSize)*this.timeBinSize;
		if(endOfTimeInterval<=0.0)endOfTimeInterval=this.timeBinSize;

		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = Id.createPersonId(event.getVehicleId());

		double actualTravelTime = event.getTime()-this.linkId2PersonIdLinkEnterTime.get(linkId).get(personId);
		this.linkId2PersonIdLinkEnterTime.get(linkId).remove(personId);
		double freeSpeedTime = this.linkId2FreeSpeedLinkTravelTime.get(linkId);
		double currentDelay =	actualTravelTime-freeSpeedTime;

		Map<Id<Person>, Double> delayForPerson = this.personId2DelaysPerTimeBin.get(endOfTimeInterval);
		
		Map<Id<Link>, Double> countTotal = this.time2linkIdLeaveCount.get(endOfTimeInterval);

		double delaySoFar = delayForPerson.get(personId);

		if(currentDelay<1.)  currentDelay=0.;

		double delayNewValue = currentDelay+delaySoFar;
		this.totalDelay+=currentDelay;	

		delayForPerson.put(personId, Double.valueOf(delayNewValue));

		double countsSoFar = countTotal.get(linkId);
		double newValue = countsSoFar + 1.;
		countTotal.put(linkId, Double.valueOf(newValue));
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double time = event.getTime();
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = Id.createPersonId(event.getVehicleId());

		Map<Id<Person>, Double> personId2LinkEnterTime = this.linkId2PersonIdLinkEnterTime.get(linkId);
		personId2LinkEnterTime.put(personId, time);
		this.linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.linkId2PersonIdLinkEnterTime.get(event.getLinkId()).remove(event.getPersonId());
	}

	public Map<Double, Map<Id<Person>, Double>> getDelayPerPersonAndTimeInterval(){
		return this.personId2DelaysPerTimeBin;
	}

	public double getTotalDelayInHours(){
		return totalDelay/3600;
	}
	
	public Map<Double, Map<Id<Link>, Double>> getTime2linkIdLeaveCount() {
		return this.time2linkIdLeaveCount;
	}
}