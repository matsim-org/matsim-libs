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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;

/**
 * @author amit
 */
public class ExperiencedDelayHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
PersonDepartureEventHandler, PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {

	public final static Logger LOG = Logger.getLogger(ExperiencedDelayHandler.class);
	
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	
	private final SortedMap<Double, Map<Id<Person>, Double>> timebin2PersonId2Delay = new TreeMap<>();
	private final Map<Double, Map<Id<Link>, Double>> timebin2LinkId2Delay = new HashMap<>();
	private final Map<Id<Link>, Map<Id<Person>, Double>> linkId2PersonIdLinkEnterTime = new HashMap<>();
	private final Map<Id<Link>, Double> linkId2FreeSpeedLinkTravelTime = new HashMap<>();
	private final Map<Double, Map<Id<Link>, Integer>> timebin2LinkIdLeaveCount = new HashMap<>();
	private double totalDelay;
	private double warnCount = 0;

	private double timeBinSize;
	private Network network;
	private final boolean isSortingForInsideMunich ;
	private final ExtendedPersonFilter pf;

	/**
	 * @param noOfTimeBins
	 * @param simulationEndTime
	 * @param scenario must have minimally network, plans and config file.
	 * @param isSortingForInsideMunich true if outside Munich city area links are not included in analysis
	 */
	public ExperiencedDelayHandler(final Scenario scenario, final int noOfTimeBins, final double simulationEndTime, final boolean isSortingForInsideMunich){
		this.isSortingForInsideMunich = isSortingForInsideMunich;
		pf = new ExtendedPersonFilter(isSortingForInsideMunich);
		if(isSortingForInsideMunich) LOG.warn("Output data will only include links which fall inside the Munich city area");
		initialize(scenario, noOfTimeBins, simulationEndTime);
	}

	public ExperiencedDelayHandler(final Scenario scenario, final int noOfTimeBins, final double simulationEndtime){
		this(scenario, noOfTimeBins, simulationEndtime, false);
		
	}
	
	private void initialize(final Scenario scenario, final int noOfTimeBins, final double simulationEndTime){
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.network = scenario.getNetwork();

		for (Link link : this.network.getLinks().values()) {
			this.linkId2PersonIdLinkEnterTime.put(link.getId(), new HashMap<Id<Person>, Double>());
			Double freeSpeedLinkTravelTime = Double.valueOf(Math.floor(link.getLength()/link.getFreespeed())+1);
			this.linkId2FreeSpeedLinkTravelTime.put(link.getId(), freeSpeedLinkTravelTime);
		}

		for(int i =0;i<noOfTimeBins;i++){
			this.timebin2PersonId2Delay.put(this.timeBinSize*(i+1), new HashMap<Id<Person>, Double>());
			this.timebin2LinkId2Delay.put(this.timeBinSize*(i+1), new HashMap<Id<Link>, Double>());
			this.timebin2LinkIdLeaveCount.put(this.timeBinSize*(i+1), new HashMap<Id<Link>, Integer>());

			for(Person person : scenario.getPopulation().getPersons().values()){
				Map<Id<Person>, Double>	delayForPerson = this.timebin2PersonId2Delay.get(this.timeBinSize*(i+1));
				delayForPerson.put(person.getId(), Double.valueOf(0.));
			}

			for(Link link : this.network.getLinks().values()) {
				Map<Id<Link>, Double>	delayOnLink = this.timebin2LinkId2Delay.get(this.timeBinSize*(i+1));
				delayOnLink.put(link.getId(), Double.valueOf(0.));
				Map<Id<Link>, Integer> countOnLink = this.timebin2LinkIdLeaveCount.get(this.timeBinSize*(i+1));
				countOnLink.put(link.getId(), Integer.valueOf(0));
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.timebin2LinkId2Delay.clear();
		this.timebin2PersonId2Delay.clear();
		LOG.info("Resetting person delays to   " + this.timebin2PersonId2Delay);
		this.linkId2PersonIdLinkEnterTime.clear();
		this.linkId2FreeSpeedLinkTravelTime.clear();
		this.timebin2LinkIdLeaveCount.clear();
		this.transitDriverPersons.clear();
		LOG.info("Resetting linkLeave counter to " + this.timebin2LinkIdLeaveCount);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = event.getPersonId();
		
		if(this.transitDriverPersons.contains(personId)) return;
		
		if(this.linkId2PersonIdLinkEnterTime.get(linkId).containsKey(personId)){
			// Person is already on the link. Cannot happen.
			throw new RuntimeException();
		} 

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
		Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());
		
		if (this.transitDriverPersons.contains(personId)) return;

		double actualTravelTime = event.getTime()-this.linkId2PersonIdLinkEnterTime.get(linkId).get(personId);
		this.linkId2PersonIdLinkEnterTime.get(linkId).remove(personId);
		double freeSpeedTime = this.linkId2FreeSpeedLinkTravelTime.get(linkId);

		double currentDelay =	actualTravelTime-freeSpeedTime;
		if(currentDelay<1.)  currentDelay=0.;
		this.totalDelay+=currentDelay;

		Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
		if( this.isSortingForInsideMunich && !pf.isCellInsideMunichCityArea(linkCoord) ) return;

		Map<Id<Person>, Double> delayForPerson = this.timebin2PersonId2Delay.get(endOfTimeInterval);
		Map<Id<Link>, Double> delayOnLink = this.timebin2LinkId2Delay.get(endOfTimeInterval);
		Map<Id<Link>, Integer> countTotal = this.timebin2LinkIdLeaveCount.get(endOfTimeInterval);

		delayForPerson.put(personId, Double.valueOf(currentDelay+delayForPerson.get(personId)));

		delayOnLink.put(linkId, Double.valueOf(currentDelay+delayOnLink.get(linkId)));

		double countsSoFar = countTotal.get(linkId);
		double newValue = countsSoFar + 1.;
		countTotal.put(linkId, Integer.valueOf((int) newValue));
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double time = event.getTime();
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());

		if(this.linkId2PersonIdLinkEnterTime.get(linkId).containsKey(personId) && warnCount==0){
			warnCount++;
			LOG.warn("Person "+personId+" is entering on link "+linkId+" two times without leaving from the same. "
					+ "Link enter times are "+this.linkId2PersonIdLinkEnterTime.get(linkId).get(personId)+" and "+time);
			LOG.warn("Reason might be : There is at least one teleport activity departing on the link (and thus derived link "
					+ "enter time) and later person is entering the link with main congested mode. In such cases, the old time will be replaced.");
			Gbl.ONLYONCE.toString();
		}

		Map<Id<Person>, Double> personId2LinkEnterTime = this.linkId2PersonIdLinkEnterTime.get(linkId);
		personId2LinkEnterTime.put(personId, time);
		this.linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(this.transitDriverPersons.remove(event.getPersonId())) return;
		this.linkId2PersonIdLinkEnterTime.get(event.getLinkId()).remove(event.getPersonId());
	}

	public SortedMap<Double, Map<Id<Person>, Double>> getDelayPerPersonAndTimeInterval(){
		return this.timebin2PersonId2Delay;
	}

	public Map<Double, Map<Id<Link>, Double>> getDelayPerLinkAndTimeInterval(){
		return this.timebin2LinkId2Delay;
	}

	public double getTotalDelayInHours(){
		return this.totalDelay/3600;
	}

	public Map<Double, Map<Id<Link>, Integer>> getTime2linkIdLeaveCount() {
		return this.timebin2LinkIdLeaveCount;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDriverPersons.add(event.getDriverId());
	}
}