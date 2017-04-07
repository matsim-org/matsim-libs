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
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;


/**
 * Determines delay of all vehicles inside a given subnetwork.
 * 
 * @author dgrether
 * @author tthunig
 *
 */
public class TtTotalDelay implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler{

	private static final Logger LOG = Logger.getLogger(TtTotalDelay.class);
	
	/** (sub)network where delay should be calculated */
	private Network network;
	private boolean considerStuckAbortDelay = false;
	
	private Map<Id<Vehicle>, Double> earliestLinkExitTimePerVeh;
	private double totalVehDelay;

	public TtTotalDelay(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.earliestLinkExitTimePerVeh = new HashMap<>();
		this.totalVehDelay = 0.0;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())){
			// for the first link every vehicle needs one second without delay
			this.earliestLinkExitTimePerVeh.put(event.getVehicleId(), event.getTime() + 1);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			Link link = this.network.getLinks().get(event.getLinkId());
			double freespeedTT = link.getLength()/link.getFreespeed();
			// this is the earliest time where matsim sets the agent to the next link
			double matsimFreespeedTT = Math.floor(freespeedTT + 1);
			this.earliestLinkExitTimePerVeh.put(event.getVehicleId(), event.getTime() + matsimFreespeedTT);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			Double earliestLinkExitTime = this.earliestLinkExitTimePerVeh.remove(event.getVehicleId());
			if (earliestLinkExitTime != null) {
				// add the number of seconds the vehicle is later as the earliest link exit time as delay
				this.totalVehDelay += event.getTime() - earliestLinkExitTime;
			}
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		// no delay occurs on the arrival link
		this.earliestLinkExitTimePerVeh.remove(event.getVehicleId());		
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		Double earliestLinkExitTime = this.earliestLinkExitTimePerVeh.remove(event.getVehicleId());
		if (this.considerStuckAbortDelay){
			if (this.network.getLinks().containsKey(event.getLinkId())) {
				if (earliestLinkExitTime != null) {
					// add the number of seconds the vehicle is later as the earliest link exit time as delay
					double stuckAbortDelay = event.getTime() - earliestLinkExitTime;
					this.totalVehDelay += stuckAbortDelay;
					LOG.warn("Add delay " + stuckAbortDelay + " of vehicle " + event.getVehicleId() + " that had a vehicleAbortsEvent on link " + event.getLinkId());
				}
			}
		}
	}

	public double getTotalDelay() {
		return totalVehDelay;
	}
	
	public void considerDelayOfStuckedOrAbortedVehicles(){
		this.considerStuckAbortDelay = true;
	}

}
