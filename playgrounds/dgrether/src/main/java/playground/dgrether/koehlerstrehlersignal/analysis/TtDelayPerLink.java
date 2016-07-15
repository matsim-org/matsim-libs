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
 * calculates the delay per link for all links of the given network
 * 
 * @author tthunig
 *
 */
public class TtDelayPerLink implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler{

	private Network network;
	private Map<Id<Vehicle>, Double> earliestLinkExitTimePerVehicle;
	private Map<Id<Link>, Double> delayPerLink;

	public TtDelayPerLink(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.earliestLinkExitTimePerVehicle = new HashMap<>();
		this.delayPerLink = new HashMap<>();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			this.earliestLinkExitTimePerVehicle.put(event.getVehicleId(), event.getTime() + 1);
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
	public void handleEvent(LinkLeaveEvent event) {		
		double earliestLinkExitTime = this.earliestLinkExitTimePerVehicle.remove(event.getVehicleId());
		if (!this.delayPerLink.containsKey(event.getLinkId())){
			this.delayPerLink.put(event.getLinkId(), 0.0);
		}
		// add the number of seconds the agent is later as the earliest link exit time as delay
		double formerLinkDelay = this.delayPerLink.get(event.getLinkId());
		this.delayPerLink.put(event.getLinkId(), formerLinkDelay + event.getTime() - earliestLinkExitTime);
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		this.earliestLinkExitTimePerVehicle.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.earliestLinkExitTimePerVehicle.remove(event.getVehicleId());
	}

	
	public Map<Id<Link>, Double> getDelayPerLink() {
		return delayPerLink;
	}

}
