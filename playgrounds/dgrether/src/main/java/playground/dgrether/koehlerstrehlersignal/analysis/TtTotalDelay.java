/* *********************************************************************** *
 * project: org.matsim.*
 * DgAverageTravelTimeSpeed
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class TtTotalDelay implements LinkEnterEventHandler, LinkLeaveEventHandler, Wait2LinkEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler{

	private static final Logger log = Logger
			.getLogger(TtTotalDelay.class);
	
	private Network network;
	private Map<Id<Vehicle>, Double> earliestLinkExitTimePerVehicle;
	private double totalDelay;

	public TtTotalDelay(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.earliestLinkExitTimePerVehicle = new HashMap<>();
		this.totalDelay = 0.0;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			Double earliestLinkExitTime = this.earliestLinkExitTimePerVehicle.remove(event.getVehicleId());
			if (earliestLinkExitTime != null) {
				// add the number of seconds the agent is later as the earliest link exit time as delay
				this.totalDelay += event.getTime() - earliestLinkExitTime;
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			Link link = this.network.getLinks().get(event.getLinkId());
			double freespeedTT = link.getLength()/link.getFreespeed();
			// this is the earliest time where matsim sets the agent to the next link
			double matsimFreespeedTT = Math.floor(freespeedTT + 1);
			this.earliestLinkExitTimePerVehicle.put(event.getVehicleId(), event.getTime() + matsimFreespeedTT);
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		this.earliestLinkExitTimePerVehicle.remove(event.getVehicleId());
		log.warn("Vehicle " + event.getVehicleId() + " got stucked at link "
				+ event.getLinkId() + ". Its delay at this link is not considered in the total delay.");
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.earliestLinkExitTimePerVehicle.remove(event.getVehicleId());		
	}

	
	public double getTotalDelay() {
		return totalDelay;
	}

	@Override
	public void handleEvent(Wait2LinkEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())){
			// for the first link every agent needs one second
			this.earliestLinkExitTimePerVehicle.put(event.getVehicleId(), event.getTime() + 1);
		}
	}

}
