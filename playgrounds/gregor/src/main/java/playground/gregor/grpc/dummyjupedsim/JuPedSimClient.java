/* *********************************************************************** *
 * project: org.matsim.*
 * JuPedSimClient.java
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

package playground.gregor.grpc.dummyjupedsim;

import io.grpc.ChannelImpl;
import io.grpc.transport.netty.NegotiationType;
import io.grpc.transport.netty.NettyChannelBuilder;

import java.util.concurrent.TimeUnit;

import org.matsim.hybrid.MATSimInterfaceServiceGrpc;
import org.matsim.hybrid.MATSimInterfaceServiceGrpc.MATSimInterfaceServiceBlockingStub;

public class JuPedSimClient {
	private final ChannelImpl channel;

	private final MATSimInterfaceServiceBlockingStub blockingStub;

	public JuPedSimClient(String host, int port) {
		this.channel = NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.PLAINTEXT).build();
		this.blockingStub = MATSimInterfaceServiceGrpc.newBlockingStub(this.channel);
	}

	public void shutdown() throws InterruptedException {
		this.channel.shutdown().awaitTerminated(5, TimeUnit.SECONDS);
	}

	public MATSimInterfaceServiceBlockingStub getBlockingStub(){
		return this.blockingStub;
	}

}
