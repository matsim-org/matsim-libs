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
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;


/**
 * @author tthunig
 *
 */
public class TtDelayPerLink implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler{

	private Network network;
	private Map<Id<Vehicle>, LinkEnterEvent> linkEnterPerVehicle;
	private Map<Id<Link>, Double> delayPerLink;

	public TtDelayPerLink(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.linkEnterPerVehicle = new HashMap<>();
		this.delayPerLink = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		//calculate delay for signalized links, so the delay caused by the signals
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			LinkEnterEvent linkEnterEvent = this.linkEnterPerVehicle.remove(event.getVehicleId());
			Id<Link> linkId = event.getLinkId();
			Link link = this.network.getLinks().get(linkId);
			double freespeedTravelTime = link.getLength()/link.getFreespeed();
			
			if (linkEnterEvent != null) {
				if (!this.delayPerLink.containsKey(linkId))
					this.delayPerLink.put(linkId, 0.0);
				
				this.delayPerLink.put(linkId, this.delayPerLink.get(linkId) + 
						(event.getTime() - linkEnterEvent.getTime() - freespeedTravelTime));
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			this.linkEnterPerVehicle.put(event.getVehicleId(), event);
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		this.linkEnterPerVehicle.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.linkEnterPerVehicle.remove(event.getVehicleId());		
	}

	
	public Map<Id<Link>, Double> getDelayPerLink() {
		return delayPerLink;
	}

}
