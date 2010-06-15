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

package playground.mrieser.core.sim.features;

import org.matsim.api.core.v01.network.Network;

import playground.mrieser.core.sim.network.api.SimNetwork;
import playground.mrieser.core.sim.network.queueNetwork.QueueNetwork;

public class QueueNetworkFeature implements NetworkFeature {

	private final QueueNetwork network;

	public QueueNetworkFeature(final Network network) {
		// convert network
		this.network = new QueueNetwork();
	}

	@Override
	public void doSimStep(double time) {
		this.network.doSimStep(time);
	}

	@Override
	public SimNetwork getSimNetwork() {
		return this.network;
	}

	@Override
	public boolean isFinished() {
		// TODO change to take road state into account
		return true;
	}

}
