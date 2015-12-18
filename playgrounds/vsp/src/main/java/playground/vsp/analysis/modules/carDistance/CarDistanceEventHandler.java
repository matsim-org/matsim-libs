/* *********************************************************************** *
 * project: org.matsim.*
 * CarDistanceEventHandler.java
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
package playground.vsp.analysis.modules.carDistance;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * @author ikaddoura, benjamin
 *
 */
public class CarDistanceEventHandler implements LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler{
	private final static Logger logger = Logger.getLogger(CarDistanceEventHandler.class);

	private Map<Id<Person>, Double> personId2CarDistance;
	private int carTrips;
	private final Network network;
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	
	// the following is not neccessary any more...see below
	private Map<Id<Person>, Id<Link>> personId2departureLinkId;
	private Map<Id<Person>, Double> depArrOnSameLinkCnt;
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	
	public CarDistanceEventHandler(Network network, PtDriverIdAnalyzer ptDriverPrefixAnalyzer) {
		this.personId2CarDistance = new HashMap<>();
		this.carTrips = 0;
		this.network = network;
		this.ptDriverIdAnalyzer = ptDriverPrefixAnalyzer;
		
		// the following is not neccessary any more...see below
		this.personId2departureLinkId = new HashMap<>();
		this.depArrOnSameLinkCnt = new HashMap<>();
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
		
		this.personId2CarDistance = new HashMap<>();
		this.carTrips = 0;
		logger.info("resetting personId2CarDistance to " + this.personId2CarDistance + " ...");
		logger.info("resetting carTrips to " + this.carTrips + " ...");
		
		// the following is not neccessary any more...see below
		this.personId2departureLinkId = new HashMap<>();
		this.depArrOnSameLinkCnt = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();
		Double linkLength_m = this.network.getLinks().get(linkId).getLength();
		if (this.ptDriverIdAnalyzer.isPtDriver(personId)){
			// pt vehicle!
		} else {
			if(this.personId2CarDistance.get(personId) == null){
				this.personId2CarDistance.put(personId, linkLength_m);
			} else {
				double distanceSoFar = this.personId2CarDistance.get(personId);
				double distanceAfterEvent = distanceSoFar + linkLength_m;
				this.personId2CarDistance.put(personId, distanceAfterEvent);
			}
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {		
		// the following is not neccessary any more...see below
		personId2departureLinkId.put(event.getPersonId(), event.getLinkId());

		if (this.ptDriverIdAnalyzer.isPtDriver(event.getPersonId())){
			// ptDriver!
		} else {
			// calculating the number of trips...
			if(event.getLegMode().equals(TransportMode.car)){
				Id<Person> personId = event.getPersonId();
				int carTripsSoFar = carTrips;
				int carTripsAfter = carTripsSoFar + 1;
				carTrips = carTripsAfter;
						
				// in order to get the number of car users right...see below
				if(this.personId2CarDistance.get(personId) == null){
				this.personId2CarDistance.put(personId, 0.0);
				} else {
					// do nothing
				}
			} else {
				// other mode
			}
		}
	}
	
	// the following is not neccessary any more...see above
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		Id<Link> linkId = event.getLinkId();

		if (this.ptDriverIdAnalyzer.isPtDriver(personId)){
			// ptDriver!
		} else {
			if(personId2departureLinkId.get(personId) == null){
				logger.warn("Person " + personId + " is arriving on link " + linkId + " without having departed anywhere before...");
			} else {
				Id<Link> departureLinkId = personId2departureLinkId.get(personId);
				if(event.getLegMode().equals(TransportMode.car)){
					if(departureLinkId.equals(linkId)){
						if(depArrOnSameLinkCnt.get(personId) == null){
							depArrOnSameLinkCnt.put(personId, 1.0);
						} else {
							double cntSoFar = depArrOnSameLinkCnt.get(personId);
							double cntAfter = cntSoFar + 1.0;
							depArrOnSameLinkCnt.put(personId, cntAfter);
						}
					}
				}
			}
		}
	}

	// the following is not neccessary any more...see above
	protected Map<Id<Person>, Double> getDepArrOnSameLinkCnt() {
		return depArrOnSameLinkCnt;
	}

	protected Map<Id<Person>, Double> getPersonId2CarDistance() {
		return this.personId2CarDistance;
	}
	
	protected int getCarTrips() {
		return this.carTrips;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}
}
