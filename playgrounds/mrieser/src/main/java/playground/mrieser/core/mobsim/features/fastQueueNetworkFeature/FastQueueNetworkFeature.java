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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import org.matsim.api.core.v01.network.Network;

import playground.mrieser.core.mobsim.api.TimestepSimEngine;
import playground.mrieser.core.mobsim.features.NetworkFeature;
import playground.mrieser.core.mobsim.network.api.MobSimNetwork;
import playground.mrieser.core.mobsim.network.api.VisNetwork;

public class FastQueueNetworkFeature implements NetworkFeature {

	private final QueueNetwork network;
	private volatile VisNetwork visNetwork;

	public FastQueueNetworkFeature(final Network network, final TimestepSimEngine simEngine) {
		this.network = QueueNetworkCreator.createQueueNetwork(network, simEngine, new SingleCPUOperator());
	}

	public FastQueueNetworkFeature(final Network network, final TimestepSimEngine simEngine, final int nOfThreads) {
		ParallelOperator operator = new ParallelOperator(nOfThreads);
		this.network = QueueNetworkCreator.createQueueNetwork(network, simEngine, operator);
		operator.setQueueNetwork(this.network);
	}

	@Override
	public void beforeMobSim() {
		this.network.beforeMobSim();
	}

	@Override
	public void doSimStep(double time) {
		this.network.doSimStep(time);
	}

	@Override
	public void afterMobSim() {
		this.network.afterMobSim();
	}

	@Override
	public MobSimNetwork getSimNetwork() {
		return this.network;
	}

	public VisNetwork getVisNetwork() {
		if (this.visNetwork == null) {
			buildVisNetwork();
		}
		return this.visNetwork;
	}

	private synchronized void buildVisNetwork() {
		if (this.visNetwork == null) {
			this.visNetwork = new VisNetworkImpl(this.network);
		}
	}

}
