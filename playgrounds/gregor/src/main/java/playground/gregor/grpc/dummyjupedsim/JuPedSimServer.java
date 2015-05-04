/* *********************************************************************** *
 * project: org.matsim.*
 * JuPedSimServer.java
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

import io.grpc.ServerImpl;
import io.grpc.transport.netty.NettyServerBuilder;

import org.apache.log4j.Logger;
import org.matsim.hybrid.ExternInterfaceServiceGrpc;

import playground.gregor.hybridsim.grpc.GRPCInternalServer;

public class JuPedSimServer implements Runnable{

	private static final Logger logger = Logger.getLogger(GRPCInternalServer.class);

	/* The port on which the server should run */
	private final int port = 9998;
	private ServerImpl server;

	private final ExternalInterfaceServiceImpl mi;

	public JuPedSimServer(ExternalInterfaceServiceImpl mi) {
		this.mi = mi;
	}

	private void start() throws Exception {
		this.server = NettyServerBuilder.forPort(this.port)
				.addService(ExternInterfaceServiceGrpc.bindService(this.mi))
				.build().start();
		logger.info("Server started, listening on " + this.port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its JVM shutdown hook.
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				JuPedSimServer.this.stop();
				System.err.println("*** server shut down");
			}
		});
	}

	private void stop() {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	@Override
	public void run() {
		try {
			start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
