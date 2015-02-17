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

package playground.gregor.dummysim;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.external.BlockingExternalInterfaceServcice;
import playground.gregor.proto.ProtoMATSimInterface;
import playground.gregor.proto.ProtoMATSimInterface.Extern2MATSim;
import playground.gregor.proto.ProtoMATSimInterface.Extern2MATSimConfirmed;
import playground.gregor.proto.ProtoMATSimInterface.ExternSimStepFinished;
import playground.gregor.proto.ProtoMATSimInterface.ExternSimStepFinishedReceived;
import playground.gregor.proto.ProtoMATSimInterface.ExternalConnect;
import playground.gregor.proto.ProtoMATSimInterface.ExternalConnectConfirmed;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternHasSpace;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternPutAgent;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternPutAgent.Agent;
import playground.gregor.proto.ProtoMATSimInterface.MATSimInterfaceService.BlockingInterface;

import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.client.DuplexTcpClientPipelineFactory;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;

public class DummySim {

	private RpcController cntr;
	private BlockingInterface service;
	private BlockingExternalInterfaceServcice serviceHandler;
	private LinkedList<Agent> agents = new LinkedList<>();

	public DummySim(BlockingInterface service, RpcController cntr,
			BlockingExternalInterfaceServcice serviceHandler) {
		this.cntr = cntr;
		this.service = service;
		this.serviceHandler = serviceHandler;
	}

	public static void main(String[] args) throws IOException,
			ServiceException, InterruptedException, BrokenBarrierException {
		if (args.length != 2) {
			System.out.println("usage: DummySim <server> <port>");
			System.exit(-1);
		}
		String serv = args[0];
		int port = Integer.parseInt(args[1]);

		PeerInfo server = new PeerInfo(serv, port);
		DuplexTcpClientPipelineFactory clientFactory = new DuplexTcpClientPipelineFactory();

		// in case client acts as server, which is the reason behind a duplex
		// connection indeed.
		RpcServerCallExecutor executor = new ThreadPoolCallExecutor(3, 100);
		clientFactory.setRpcServerCallExecutor(executor);

		clientFactory.setConnectResponseTimeoutMillis(1000);

		// clientFactory.getRpcServiceRegistry().registerService();

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup());
		bootstrap.handler(clientFactory);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
		bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
		bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);

		RpcClientChannel channel = clientFactory.peerWith(server, bootstrap);

		BlockingInterface service = ProtoMATSimInterface.MATSimInterfaceService
				.newBlockingStub(channel);
		RpcController cntr = channel.newRpcController();
		BlockingExternalInterfaceServcice serviceHandler = new BlockingExternalInterfaceServcice();
		clientFactory.getRpcServiceRegistry().registerService(
				ProtoMATSimInterface.ExternInterfaceService
						.newReflectiveBlockingService(serviceHandler));

		new DummySim(service, cntr, serviceHandler).run();

	}

	private void run() throws ServiceException, InterruptedException,
			BrokenBarrierException {

		double stepSize = 0.01;

		// setup
		CyclicBarrier simStepBarrier = new CyclicBarrier(2);
		this.serviceHandler.setSimStepBarrier(simStepBarrier);
		this.serviceHandler.setDummySim(this);

		ExternalConnect connectReq = ExternalConnect.newBuilder().build();
		// request connection
		ExternalConnectConfirmed connect = this.service.reqExternalConnect(
				this.cntr, connectReq);
		// connection established
		while (true) {

			simStepBarrier.await();
			for (double time = this.serviceHandler.getFrom(); time < this.serviceHandler
					.getTo(); time += stepSize) {
				// do something
				Iterator<Agent> it = this.agents.iterator();
				while (it.hasNext()) {
					Agent next = it.next();
					// check whether agent wants to leave
					if (MatsimRandom.getRandom().nextDouble() > 0.99) {
						ProtocolStringList nl = next.getNodesList();
						int sz = nl.size();
						String exNode = nl.get(sz - 1);
						Extern2MATSim enterMATSimRequest = Extern2MATSim
								.newBuilder()
								.setAgent(
										Extern2MATSim.Agent.newBuilder()
												.setId(next.getId())
												.setLeaveNode(exNode).build())
								.build();
						// check whether MATSim is able to take agent
						Extern2MATSimConfirmed resp = this.service
								.reqExtern2MATSim(this.cntr, enterMATSimRequest);
						if (resp.hasAccepted()) {
							// if success remove agent from DummySim
							it.remove();
						}
					}
				}
			}
			ExternSimStepFinished reqNextSimStep = ExternSimStepFinished
					.newBuilder().setTime(this.serviceHandler.getTo()).build();
			ExternSimStepFinishedReceived reqNextSimStepResp = this.service
					.reqExternSimStepFinished(cntr, reqNextSimStep);

		}

	}

	public void handle(MATSim2ExternPutAgent request) {
		Agent a = request.getAgent();
		this.agents.add(a);

	}

	public boolean handle(MATSim2ExternHasSpace request) {

		String node = request.getNodeId();
		// test whether agent can enter DummySim at node right now
		if (MatsimRandom.getRandom().nextDouble() > 0.8) {
			return true;
		}
		return false;
	}

}
