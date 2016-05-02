/* *********************************************************************** *
 * project: org.matsim.*
 * Client.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.hybridsim.grpc;


import io.grpc.internal.ManagedChannelImpl;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;

import org.matsim.contrib.hybridsim.proto.HybridSimulationGrpc;

import java.util.concurrent.TimeUnit;

public class GRPCExternalClient {

	private HybridSimulationGrpc.HybridSimulationBlockingStub blockingStub;
	private final ManagedChannelImpl channel;

	public GRPCExternalClient(String host, int port) {

		this.channel = NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.PLAINTEXT).build();
	this.blockingStub = HybridSimulationGrpc.newBlockingStub(this.channel);
	}

	public void shutdown()  {
		try {
			this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public HybridSimulationGrpc.HybridSimulationBlockingStub getBlockingStub() {
		return this.blockingStub;
	}
}
