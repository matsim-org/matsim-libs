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
import org.apache.log4j.Logger;
import org.matsim.contrib.hybridsim.interfacedef.ExternInterfaceServiceGrpc;

import java.util.concurrent.TimeUnit;

public class GRPCExternalClient {
	private static final Logger log = Logger.getLogger(GRPCExternalClient.class);


	private final ExternInterfaceServiceGrpc.ExternInterfaceServiceBlockingStub blockingStub;
	private final ManagedChannelImpl channel;

	public GRPCExternalClient(String host, int port) {
		this.channel = NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.PLAINTEXT).build();
	this.blockingStub = ExternInterfaceServiceGrpc.newBlockingStub(this.channel);
	}

	public void shutdown() throws InterruptedException {
		this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}
	
	public ExternInterfaceServiceGrpc.ExternInterfaceServiceBlockingStub getBlockingStub(){
		return this.blockingStub;
	}
	
}
