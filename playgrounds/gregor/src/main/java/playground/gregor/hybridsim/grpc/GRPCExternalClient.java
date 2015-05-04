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

package playground.gregor.hybridsim.grpc;

import io.grpc.ChannelImpl;
import io.grpc.transport.netty.NegotiationType;
import io.grpc.transport.netty.NettyChannelBuilder;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.matsim.hybrid.ExternInterfaceServiceGrpc;
import org.matsim.hybrid.ExternInterfaceServiceGrpc.ExternInterfaceServiceBlockingStub;

public class GRPCExternalClient {
	private static final Logger log = Logger.getLogger(GRPCExternalClient.class);

	private final ChannelImpl channel;

	private final ExternInterfaceServiceBlockingStub blockingStub;

	public GRPCExternalClient(String host, int port, CyclicBarrier cb) {
		this.channel = NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.PLAINTEXT).build();
	this.blockingStub = ExternInterfaceServiceGrpc.newBlockingStub(this.channel);
	try {
		cb.await();
	} catch (InterruptedException | BrokenBarrierException e) {
		throw new RuntimeException(e);
	}
	log.info("client up and running.");
	}

	public void shutdown() throws InterruptedException {
		this.channel.shutdown().awaitTerminated(5, TimeUnit.SECONDS);
	}
	
	public ExternInterfaceServiceBlockingStub getBlockingStub(){
		return this.blockingStub;
	}
	
}
