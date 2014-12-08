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

import org.apache.log4j.Logger;

import playground.gregor.proto.ProtoFrame.CtrlMsg;
import playground.gregor.proto.ProtoFrame.CtrlMsgRsp;
import playground.gregor.proto.ProtoFrame.Frame;
import playground.gregor.proto.ProtoFrame.FrameRqst;
import playground.gregor.proto.ProtoFrame.FrameServerService.BlockingInterface;
import playground.gregor.proto.ProtoScenario.Scenario;
import playground.gregor.proto.ProtoScenario.ScnReq;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

public class BlockingVisServiceImpl implements BlockingInterface {

	private static final Logger log = Logger
			.getLogger(BlockingVisServiceImpl.class);

	int invoc = 0;

	private VisServer srv;

	private final VisRequestHandler reqHandler;

	public BlockingVisServiceImpl(VisServer srv, VisRequestHandler reqHandler) {
		this.srv = srv;
		this.reqHandler = reqHandler;
	}

	@Override
	public Frame reqFrame(RpcController controller, FrameRqst request)
			throws ServiceException {
		// log.info(request);
		// request.g
		return this.reqHandler.handleRequest(request);

		// try {
		// return srv.cache.take();
		// } catch (InterruptedException e) {
		// throw new RuntimeException(e);
		// }
	}

	@Override
	public Scenario reqScn(RpcController controller, ScnReq request)
			throws ServiceException {

		return this.srv.scenario;
	}

	@Override
	public CtrlMsgRsp ctrl(RpcController controller, CtrlMsg request)
			throws ServiceException {

		if (request.getCtrlMsgTyp() == CtrlMsg.Type.REGISTER) {
			this.reqHandler.registerClient(request.getId());
		}
		return CtrlMsgRsp.newBuilder().build();
	}

}
