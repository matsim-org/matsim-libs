/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.michalm.drt.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.tasks.DrtDriveWithPassengersTask;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.michalm.drt.run.DrtConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class DrtPassengerStats implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler, ActivityEndEventHandler{

	
	
	final private DescriptiveStatistics waitTimes = new DescriptiveStatistics();
	final private DescriptiveStatistics rideDistances = new DescriptiveStatistics();
	final private List<Tuple<Double,Double>> beelineVsRideDistances = new ArrayList();
	final private Map<Id<Person>,Double> departureTimes = new HashMap<>();
	final private Map<Id<Person>,Id<Link>> departureLinks = new HashMap<>();
	final private List<DrtDepature> drtDepatures = new ArrayList<>();
	final private Map<Id<Vehicle>,Map<Id<Person>,MutableDouble>> inVehicleDistance = new HashMap<>();
	
	final private Network network;
	final private DrtConfigGroup drtConfigGroup;
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	
	/**
	 * 
	 */
	@Inject
	public DrtPassengerStats(Network network, Config config) {
		this.network = network;
		this.drtConfigGroup = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
	}
	
	@Override
	public void reset(int iteration) {
		waitTimes.clear();
		rideDistances.clear();
		beelineVsRideDistances.clear();
		waitTimes.clear();
		this.drtDepatures.clear();
		this.rideDistances.clear();
	}
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)){
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());
			this.inVehicleDistance.put(vid, new HashMap<>());
		}
	}
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {

		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(DrtConfigGroup.DRTMODE)){
			this.departureTimes.put(event.getPersonId(), event.getTime());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.departureTimes.containsKey(event.getPersonId())){
			double departureTime = this.departureTimes.remove(event.getPersonId());
			double waitTime = event.getTime() - departureTime;
			this.waitTimes.addValue(waitTime);
			Id<Link> departureLink = this.departureLinks.remove(event.getPersonId());
			DrtDepature departure = new DrtDepature(departureTime, event.getPersonId(), event.getVehicleId(), departureLink, waitTime);
			this.drtDepatures.add(departure);
			this.inVehicleDistance.get(event.getVehicleId()).put(event.getPersonId(), new MutableDouble());
		}
	}

	
	public class DrtDepature implements Comparable<DrtDepature>{
		private final double departureTime; 
		private final Id<Person> person; 
		private final Id<Vehicle> vehicle; 
		private final Id<Link> fromLinkId; 
		private final double waitTime;

		DrtDepature(double departureTime, Id<Person> person, Id<Vehicle> vehicle, Id<Link> fromLinkId,
				double waitTime) {
			this.departureTime = departureTime;
			this.person = person;
			this.vehicle = vehicle;
			this.fromLinkId = fromLinkId;
			this.waitTime = waitTime;
		}

		public Double getDepartureTime() {
			return departureTime;
		}

		public Id<Person> getPerson() {
			return person;
		}

		public Id<Vehicle> getVehicle() {
			return vehicle;
		}

		public Id<Link> getFromLinkId() {
			return fromLinkId;
		}

		public double getWaitTime() {
			return waitTime;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(DrtDepature o) {
			return getDepartureTime().compareTo(o.getDepartureTime());
		}
		
		
		
	}


	
}
