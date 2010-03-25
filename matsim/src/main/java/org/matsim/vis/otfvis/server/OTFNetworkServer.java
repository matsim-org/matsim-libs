/* *********************************************************************** *
 * project: org.matsim.*
 * OTFTVehServer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.server;

import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Collection;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFQSimServerQuadBuilder;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;

/**
 * OTFNetworkServer is a Server that reads from the network.xml file format.
 *
 * @author dstrippgen
 * @author mrieser
 */
public class OTFNetworkServer implements OTFServerRemote {
	private static final int BUFFERSIZE = 100000000;

	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();
	private final OTFServerQuad2 quad;

	private final ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);

	public OTFNetworkServer(String netFileName) {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		NetworkLayer net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFileName);
		QNetwork qnet = new QNetwork(net);

		OTFConnectionManager connect = new OTFConnectionManager();
		connect.connectQLinkToWriter(OTFLinkLanesAgentsNoParkingHandler.Writer.class);

		OTFQSimServerQuadBuilder quadBuilder = new OTFQSimServerQuadBuilder(qnet);

		this.quad = quadBuilder.createAndInitOTFServerQuad(connect);
		this.quad.initQuadTree(connect);
		this.quad.addAdditionalElement(this.writer);
	}

	@Override
	public int getLocalTime() throws RemoteException {
		return 0;
	}

	@Override
	public OTFServerQuad2 getQuad(String id, OTFConnectionManager connect) throws RemoteException {
		return this.quad;
	}

	@Override
	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		this.buf.position(0);
		this.quad.writeConstData(this.buf);
		byte [] result;
		synchronized (this.buf) {
			result = this.buf.array();
		}
		return result;
	}

	@Override
	public byte[] getQuadDynStateBuffer(String id, Rect bounds) throws RemoteException {
		this.buf.position(0);
		this.quad.writeDynData(null, this.buf);
		byte [] buffer = new byte[this.buf.position()+1];
		System.arraycopy(this.buf.array(), 0, buffer, 0, buffer.length);
		return buffer;
	}

	@Override
	public boolean isLive() throws RemoteException {
		return false;
	}

	@Override
	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		return false;
	}

	public Collection<Double> getTimeSteps() throws RemoteException {
		return null;
	}

	@Override
	public void toggleShowParking() throws RemoteException {
		OTFLinkAgentsHandler.showParked = !OTFLinkAgentsHandler.showParked;
	}

	@Override
	public OTFVisConfig getOTFVisConfig() throws RemoteException {
		return new OTFVisConfig();
	}
	
}
