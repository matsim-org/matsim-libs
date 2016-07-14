/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.vis;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.gregor.proto.ProtoFrame;
import playground.gregor.proto.ProtoScenario;
import playground.gregor.proto.ProtoScenario.Scenario.Network.Builder;

import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import com.googlecode.protobuf.pro.duplex.server.DuplexTcpServerPipelineFactory;
import com.googlecode.protobuf.pro.duplex.util.RenamingThreadFactoryProxy;

public class VisServer {

	private final BlockingVisServiceImpl service;
	private Scenario sc;
	public playground.gregor.proto.ProtoScenario.Scenario scenario;

	public VisServer(Scenario sc, VisRequestHandler vrh) {
		this.sc = sc;
		initScenario();
		this.service = new BlockingVisServiceImpl(this, vrh);
		initServer();
	}

	private void initScenario() {
		Builder net = ProtoScenario.Scenario.Network.newBuilder();
		List<ProtoScenario.Scenario.Network.Node> nodes = new ArrayList<>();
		playground.gregor.proto.ProtoScenario.Scenario.Network.Node.Builder nb = ProtoScenario.Scenario.Network.Node
				.newBuilder();
		for (Node n : this.sc.getNetwork().getNodes().values()) {
			nodes.add(nb.setId(n.getId().toString()).setX(n.getCoord().getX())
					.setY(n.getCoord().getY()).build());
		}
		List<ProtoScenario.Scenario.Network.Link> links = new ArrayList<>();
		playground.gregor.proto.ProtoScenario.Scenario.Network.Link.Builder lb = ProtoScenario.Scenario.Network.Link
				.newBuilder();
		for (Link l : this.sc.getNetwork().getLinks().values()) {
			links.add(lb.setCapacity(l.getCapacity())
					.setFromNodeId(l.getFromNode().getId().toString())
					.setToNodeId(l.getToNode().getId().toString())
					.setId(l.getId().toString()).setLanes(l.getNumberOfLanes())
					.setLength(l.getLength()).setFreespeed(l.getFreespeed())
					.build());
		}
		net.addAllNodes(nodes).addAllLinks(links);
		this.scenario = ProtoScenario.Scenario.newBuilder()
				.setCrs(this.sc.getConfig().global().getCoordinateSystem())
				.setNet(net.build()).build();

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
				ProtoFrame.FrameServerService
						.newReflectiveBlockingService(this.service));

		bootstrap.bind();

	}

}
