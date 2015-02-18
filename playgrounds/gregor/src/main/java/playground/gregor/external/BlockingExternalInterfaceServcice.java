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

import playground.gregor.dummysim.DummySim;
import playground.gregor.proto.ProtoMATSimInterface.ExternAfterSim;
import playground.gregor.proto.ProtoMATSimInterface.ExternAfterSimConfirmed;
import playground.gregor.proto.ProtoMATSimInterface.ExternDoSimStep;
import playground.gregor.proto.ProtoMATSimInterface.ExternDoSimStepReceived;
import playground.gregor.proto.ProtoMATSimInterface.ExternInterfaceService.BlockingInterface;
import playground.gregor.proto.ProtoMATSimInterface.ExternOnPrepareSim;
import playground.gregor.proto.ProtoMATSimInterface.ExternOnPrepareSimConfirmed;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternHasSpace;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternHasSpaceConfirmed;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternPutAgent;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternPutAgentConfirmed;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

public class BlockingExternalInterfaceServcice implements BlockingInterface {

	private double from;
	private double to;
	private CyclicBarrier simStepBarrier;
	private DummySim sim;

	public double getFrom() {
		return this.from;
	}

	public double getTo() {
		return this.to;
	}

	public void setSimStepBarrier(CyclicBarrier b) {
		this.simStepBarrier = b;
	}

	public void setDummySim(DummySim sim) {
		this.sim = sim;
	}

	@Override
	public MATSim2ExternPutAgentConfirmed reqMATSim2ExternPutAgent(
			RpcController controller, MATSim2ExternPutAgent request)
			throws ServiceException {
		this.sim.handle(request);

		return MATSim2ExternPutAgentConfirmed.newBuilder().build();
	}

	@Override
	public MATSim2ExternHasSpaceConfirmed reqMATSim2ExternHasSpace(
			RpcController controller, MATSim2ExternHasSpace request)
			throws ServiceException {

		return MATSim2ExternHasSpaceConfirmed.newBuilder()
				.setHasSpace(this.sim.handle(request)).build();
	}

	@Override
	public ExternDoSimStepReceived reqExternDoSimStep(RpcController controller,
			ExternDoSimStep request) throws ServiceException {
		this.from = request.getFromTime();
		this.to = request.getToTime();
		try {
			this.simStepBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
		return ExternDoSimStepReceived.newBuilder().build();
	}

	@Override
	public ExternOnPrepareSimConfirmed reqExternOnPrepareSim(
			RpcController controller, ExternOnPrepareSim request)
			throws ServiceException {
		this.sim.onPrepareSim(request);
		return ExternOnPrepareSimConfirmed.newBuilder().build();
	}

	@Override
	public ExternAfterSimConfirmed reqExternAfterSim(RpcController controller,
			ExternAfterSim request) throws ServiceException {
		this.sim.afterSim(request);
		return ExternAfterSimConfirmed.newBuilder().build();
	}

}
