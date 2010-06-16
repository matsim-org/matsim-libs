/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.network.queueNetwork;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.Steppable;

import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.network.api.SimLink;

/*package*/ class QueueLink implements SimLink, Steppable {

	private final QueueNetwork network;
	private final Link link;

	public QueueLink(final Link link, final QueueNetwork network) {
		this.link = link;
		this.network = network;
	}

	@Override
	public void addVehicle(SimVehicle vehicle, double position, double priority) {
		// TODO Auto-generated method stub

	}

	@Override
	public void continueVehicle(SimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	public double removeVehicle(SimVehicle vehicle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void stopVehicle(SimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSimStep(double time) {
		// TODO Auto-generated method stub

	}

	@Override
	public Id getId() {
		return this.link.getId();
	}

}
