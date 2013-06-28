/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.optimization.operatorProfitModel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author Ihab
 *
 */
public class LinksEventHandler implements LinkLeaveEventHandler, LinkEnterEventHandler {
	private double vehicleKm;
	private final Network network;
	
	public LinksEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.vehicleKm = 0.0;
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id vehicleId = event.getVehicleId();
		if (vehicleId.toString().contains("bus")){
			// vehicleKm
			this.vehicleKm = this.vehicleKm + network.getLinks().get(event.getLinkId()).getLength() / 1000;
		}
		else {}		
	}

	public double getVehicleKm() {
		return this.vehicleKm;
	}

}
