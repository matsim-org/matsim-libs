/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAverageTolledTripLength.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;

import javax.inject.Inject;
import java.util.TreeMap;

/**
 * Calculates the distance of a trip which occurred on tolled links.
 * Requires roadpricing to be on.
 *
 * @author mrieser
 */
public class CalcAverageTolledTripLength implements LinkEnterEventHandler, PersonArrivalEventHandler {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CalcAverageTolledTripLength.class);

	private double sumLength = 0.0;
	private int cntTrips = 0;
	private RoadPricingScheme scheme = null;
	private Network network = null;
	private TreeMap<Id, Double> agentDistance = null;
	
	private static Double zero = 0.0;

    @Inject
	public CalcAverageTolledTripLength(final Network network, final RoadPricingScheme scheme) {
		this.scheme = scheme;
		this.network = network;
		this.agentDistance = new TreeMap<>();
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		
		// getting the (monetary? generalized?) cost of the link
		Cost cost = this.scheme.getLinkCostInfo(event.getLinkId(), event.getTime(), event.getDriverId(), event.getVehicleId() );
		
		if (cost != null) {
			// i.e. if there is a toll on the link
			
			Link link = this.network.getLinks().get(event.getLinkId());
			if (link != null) {
				
				// get some distance that has been accumulated (how?) up to this point:
				Double length = this.agentDistance.get(event.getDriverId());
				
				// if nothing has been accumlated so far, initialize this at zero:
				if (length == null) {
					length = zero;
				}
				
				// add the new length to the already accumulated length:
				length = length + link.getLength();
				
				// put the result again in the "memory":
				this.agentDistance.put(event.getDriverId(), length);
			}
		}
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		// at arrival of the agent ...
		
		// get the accumulated "tolled" length from the agent
		Double length = this.agentDistance.get(event.getPersonId());
		if (length != null) {
			// if this is not zero, accumulate it into some global accumulated length ...
			this.sumLength += length;
			
			// ... and reset the agent-individual accumlated length to zero:
			this.agentDistance.put(event.getPersonId(), zero);
			this.cntTrips++;
		}

		// count _all_ finished trips (independent off toll payment):
//		this.cntTrips++;
	}

	@Override
	public void reset(final int iteration) {
		this.sumLength = 0.0;
		this.cntTrips = 0;
	}

	public double getAverageTripLength() {
		// public is currently needed. kai, sep'13

		if (this.cntTrips == 0) return 0;
//		log.warn("NOTE: The result of this calculation has been changed from 'av over all trips' to 'av over tolled trips'.  kai/benjamin, apr'10") ;
		// commenting this out.  kai, mar'12
		return (this.sumLength / this.cntTrips);
	}
}
