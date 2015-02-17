/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.gregor.external;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import playground.gregor.proto.ProtoMATSimInterface;

import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import com.googlecode.protobuf.pro.duplex.server.DuplexTcpServerPipelineFactory;
import com.googlecode.protobuf.pro.duplex.util.RenamingThreadFactoryProxy;

public class Server {

	private BlockingMATSimInterfaceService service;

	public Server(BlockingMATSimInterfaceService service) {
		this.service = service;
		initServer();
	}

	private void initServer() {

		PeerInfo server = new PeerInfo("localhost", 9090);

		RpcServerCallExecutor executor = new ThreadPoolCallExecutor(3, 200);

		DuplexTcpServerPipelineFactory serverFactory = new DuplexTcpServerPipelineFactory(
				server);
		serverFactory.setRpcServerCallExecutor(executor);

		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(
				new NioEventLoopGroup(0, new RenamingThreadFactoryProxy("boss",
						Executors.defaultThreadFactory())),
				new NioEventLoopGroup(0, new RenamingThreadFactoryProxy(
						"worker", Executors.defaultThreadFactory())));
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.childHandler(serverFactory);
		bootstrap.localAddress(server.getPort());
		bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
		bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);
		bootstrap.childOption(ChannelOption.SO_RCVBUF, 1048576);
		bootstrap.childOption(ChannelOption.SO_SNDBUF, 1048576);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);

		serverFactory.getRpcServiceRegistry().registerService(
				ProtoMATSimInterface.MATSimInterfaceService
						.newReflectiveBlockingService(this.service));

		bootstrap.bind();

		// this.service.reqExtern2MATSim(, null)
	}

	public static void main(String[] args) {
		CyclicBarrier b = new CyclicBarrier(2);
		CyclicBarrier c = new CyclicBarrier(2);
		BlockingMATSimInterfaceService s = new BlockingMATSimInterfaceService(
				b, c, null);
		Server serv = new Server(s);
	}
}
