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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import playground.gregor.proto.ProtoMATSimInterface.Extern2MATSim;
import playground.gregor.proto.ProtoMATSimInterface.Extern2MATSimConfirmed;
import playground.gregor.proto.ProtoMATSimInterface.ExternSimStepFinished;
import playground.gregor.proto.ProtoMATSimInterface.ExternSimStepFinishedReceived;
import playground.gregor.proto.ProtoMATSimInterface.ExternalConnect;
import playground.gregor.proto.ProtoMATSimInterface.ExternalConnectConfirmed;
import playground.gregor.proto.ProtoMATSimInterface.MATSimInterfaceService.BlockingInterface;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.execute.ServerRpcController;

public class BlockingMATSimInterfaceService implements BlockingInterface {

	private CyclicBarrier startupBarrier;
	private CyclicBarrier simStepBarrier;
	private ExternalEngine engine;

	public BlockingMATSimInterfaceService(CyclicBarrier startupBarrier,
			CyclicBarrier simStepBarrier, ExternalEngine externalEngine) {
		this.startupBarrier = startupBarrier;
		this.engine = externalEngine;
		this.simStepBarrier = simStepBarrier;
	}

	@Override
	public ExternSimStepFinishedReceived reqExternSimStepFinished(
			RpcController controller, ExternSimStepFinished request)
			throws ServiceException {
		try {
			this.simStepBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
		return ExternSimStepFinishedReceived.newBuilder().build();
	}

	@Override
	public ExternalConnectConfirmed reqExternalConnect(
			RpcController controller, ExternalConnect request)
			throws ServiceException {

		this.engine.setRpcController((ServerRpcController) controller);
		try {
			this.startupBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
		return ExternalConnectConfirmed.newBuilder().build();
	}

	@Override
	public Extern2MATSimConfirmed reqExtern2MATSim(RpcController controller,
			Extern2MATSim request) throws ServiceException {
		return Extern2MATSimConfirmed.newBuilder()
				.setAccepted(this.engine.tryAddAgent(request)).build();
	}

}
