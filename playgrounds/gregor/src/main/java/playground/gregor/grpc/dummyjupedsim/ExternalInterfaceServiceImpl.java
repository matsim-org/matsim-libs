/* *********************************************************************** *
 * project: org.matsim.*
 * ExternalInterfaceServiceImpl.java
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

import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.matsim.hybrid.ExternInterfaceServiceGrpc.ExternInterfaceService;
import org.matsim.hybrid.MATSimInterface.*;

public class ExternalInterfaceServiceImpl implements ExternInterfaceService{

	private static final Logger log = Logger.getLogger(ExternalInterfaceServiceImpl.class);
	private final DummyJuPedSim sim;
	
	public ExternalInterfaceServiceImpl(DummyJuPedSim dummyJuPedSim) {
		this.sim = dummyJuPedSim;
	}

	@Override
	public void reqMATSim2ExternHasSpace(MATSim2ExternHasSpace request,
			StreamObserver<MATSim2ExternHasSpaceConfirmed> responseObserver) {
//		log.info("has space called for node" + request.getNodeId());
		MATSim2ExternHasSpaceConfirmed resp = MATSim2ExternHasSpaceConfirmed.newBuilder().setHasSpace(true).build();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();
	}

	@Override
	public void reqMATSim2ExternPutAgent(MATSim2ExternPutAgent request,
			StreamObserver<MATSim2ExternPutAgentConfirmed> responseObserver) {
		this.sim.putAgent(request);
		MATSim2ExternPutAgentConfirmed resp = MATSim2ExternPutAgentConfirmed.newBuilder().build();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();
		
	}

	@Override
	public void reqExternDoSimStep(ExternDoSimStep request,
			StreamObserver<ExternDoSimStepReceived> responseObserver) {
		this.sim.doSimstep(request);
		ExternDoSimStepReceived resp = ExternDoSimStepReceived.newBuilder().build();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();		
	}

	@Override
	public void reqExternOnPrepareSim(ExternOnPrepareSim request,
			StreamObserver<ExternOnPrepareSimConfirmed> responseObserver) {
		this.sim.onPrepareSim(request);
		ExternOnPrepareSimConfirmed resp = ExternOnPrepareSimConfirmed.newBuilder().build();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();		
		
	}

	@Override
	public void reqExternAfterSim(ExternAfterSim request,
			StreamObserver<ExternAfterSimConfirmed> responseObserver) {
		this.sim.afterSim(request);
		ExternAfterSimConfirmed resp = ExternAfterSimConfirmed.newBuilder().build();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();		
	}

}
