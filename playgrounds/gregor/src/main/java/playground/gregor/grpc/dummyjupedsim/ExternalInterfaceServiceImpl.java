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
import org.matsim.hybrid.MATSimInterface.ExternAfterSim;
import org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed;
import org.matsim.hybrid.MATSimInterface.ExternDoSimStep;
import org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived;
import org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim;
import org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed;

public class ExternalInterfaceServiceImpl implements ExternInterfaceService{

	private static final Logger log = Logger.getLogger(ExternalInterfaceServiceImpl.class);
	
	@Override
	public void reqMATSim2ExternHasSpace(MATSim2ExternHasSpace request,
			StreamObserver<MATSim2ExternHasSpaceConfirmed> responseObserver) {
		log.info("has space called for node" + request.getNodeId());
		MATSim2ExternHasSpaceConfirmed resp = MATSim2ExternHasSpaceConfirmed.newBuilder().setHasSpace(true).build();
		responseObserver.onValue(resp);
		responseObserver.onCompleted();
	}

	@Override
	public void reqMATSim2ExternPutAgent(MATSim2ExternPutAgent request,
			StreamObserver<MATSim2ExternPutAgentConfirmed> responseObserver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reqExternDoSimStep(ExternDoSimStep request,
			StreamObserver<ExternDoSimStepReceived> responseObserver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reqExternOnPrepareSim(ExternOnPrepareSim request,
			StreamObserver<ExternOnPrepareSimConfirmed> responseObserver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reqExternAfterSim(ExternAfterSim request,
			StreamObserver<ExternAfterSimConfirmed> responseObserver) {
		// TODO Auto-generated method stub
		
	}

}
