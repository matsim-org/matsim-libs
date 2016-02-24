/* *********************************************************************** *
 * project: org.matsim.*
 * Server.java
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


import io.grpc.internal.ServerImpl;
import io.grpc.netty.NettyServerBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.hybridsim.interfacedef.MATSimInterfaceServiceGrpc;

import java.util.concurrent.CyclicBarrier;

public class GRPCInternalServer implements Runnable{

	private static final Logger logger = Logger.getLogger(GRPCInternalServer.class);

	/* The port on which the server should run */
	private final int port = 9999;


	private final MATSimInterfaceServiceGrpc.MATSimInterfaceService mi;

	private final CyclicBarrier startupBarrier;
	private ServerImpl server;

	public GRPCInternalServer(MATSimInterfaceServiceGrpc.MATSimInterfaceService mi, CyclicBarrier startupBarrier) {
		Logger.getLogger("io.netty").setLevel(Level.OFF);
		this.mi = mi;
		this.startupBarrier = startupBarrier;
	}

	@Override
	public void run() {
		try {
			start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void start() throws Exception {
		this.server = NettyServerBuilder.forPort(this.port)
				.addService(MATSimInterfaceServiceGrpc.bindService(this.mi))
				.build().start();
		logger.info("Server started, listening on " + this.port);
		this.startupBarrier.await();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its JVM shutdown hook.
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				GRPCInternalServer.this.stop();
				System.err.println("*** server shut down");
			}
		});

	}

	private void stop() {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	public void shutdown() {
		this.stop();
	}

}
