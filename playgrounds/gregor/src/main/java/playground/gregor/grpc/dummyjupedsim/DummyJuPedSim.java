/* *********************************************************************** *
 * project: org.matsim.*
 * DummyJuPedSim.java
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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.hybrid.MATSimInterface;
import org.matsim.hybrid.MATSimInterface.*;
import org.matsim.hybrid.MATSimInterface.Extern2MATSim.Agent.Builder;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent.Agent;
import org.matsim.hybrid.MATSimInterfaceServiceGrpc.MATSimInterfaceServiceBlockingStub;
import playground.gregor.hybridsim.grpc.ExternalSim;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;

public final class DummyJuPedSim implements ExternalSim{


	private final ConcurrentLinkedQueue<Agent> agentQ = new ConcurrentLinkedQueue<>();
	private final CyclicBarrier simStepBarrier = new CyclicBarrier(2);
	private JuPedSimClient c;
	private boolean run = true;
	private ExternDoSimStep currentStep;
	private JuPedSimServer server;

	public DummyJuPedSim() {
		Logger.getLogger("io.netty").setLevel(Level.OFF);

	}

	public static void main(String[] args) {
		new Thread(new DummyJuPedSim()).start();
	}

	@Override
	public void run() {
		{
			ExternalInterfaceServiceImpl ext = new ExternalInterfaceServiceImpl(this);
			this.server = new JuPedSimServer(ext);
			Thread t1 = new Thread(this.server);
			t1.start();

			this.c = new JuPedSimClient("localhost", 9999);
			MATSimInterfaceServiceBlockingStub bs = this.c.getBlockingStub();
			ExternalConnect req = MATSimInterface.ExternalConnect.newBuilder().setHost("localhost").setPort(9998).build();
			ExternalConnectConfirmed resp = bs.reqExternalConnect(req);
		}
		double stepSize = 0.01;
		double time = 0;
		while (this.run ) {
			try {
				this.simStepBarrier.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}
			if (!this.run) {
				break;
			}
			for (time = this.currentStep.getFromTime(); time < this.currentStep.getToTime(); time += stepSize) {
				Agent a;
				if (MatsimRandom.getRandom().nextDouble() < 0.02 && (a = this.agentQ.peek()) != null){
					String l = a.getLeaveNode();
					Builder ab = MATSimInterface.Extern2MATSim.Agent.newBuilder();
					ab.setId(a.getId()).setLeaveNode(l);
					Extern2MATSim req = MATSimInterface.Extern2MATSim.newBuilder().setAgent(ab).build();
					MATSimInterfaceServiceBlockingStub stub = this.c.getBlockingStub();
					Extern2MATSimConfirmed resp = stub.reqExtern2MATSim(req);
					if (resp.getAccepted()) {
						this.agentQ.poll();
					}
				}
			}
			MATSimInterfaceServiceBlockingStub stub = this.c.getBlockingStub();
			ExternSimStepFinished req = ExternSimStepFinished.newBuilder().setTime(time).build();
			stub.reqExternSimStepFinished(req);

		}
		System.err.println("done!");

	}

	public void putAgent(MATSim2ExternPutAgent request) {
		//		request.getAgent().get
		this.agentQ.add(request.getAgent());

	}

	public void doSimstep(ExternDoSimStep request) {
		this.currentStep = request;
		try {
			this.simStepBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}

	public void onPrepareSim(ExternOnPrepareSim request) {
		// TODO Auto-generated method stub

	}

	public void afterSim(ExternAfterSim request) {
		this.run = false;
		try {
			this.simStepBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
		this.server.stop();
		try {
			this.c.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

}
