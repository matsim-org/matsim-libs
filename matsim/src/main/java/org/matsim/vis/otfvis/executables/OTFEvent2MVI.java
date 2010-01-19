/* *********************************************************************** *
 * project: org.matsim.*
 * OTFEvent2MVI.java
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

package org.matsim.vis.otfvis.executables;

import java.io.File;
import java.io.IOException;

import org.matsim.core.gbl.Gbl;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.run.Events2Snapshot;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFFileWriterQSimConnectionManagerFactory;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFQSimServerQuadBuilder;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;

/**
 * This is a standalone executable class for converting a event-file to a .mvi file.
 * This is called by org.matsim.run.otfvis.
 * 
 * @author dstrippgen
 *
 */
public class OTFEvent2MVI extends OTFFileWriter {
	private final String eventFileName;

	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();

	private QueueNetwork network;

	public OTFEvent2MVI(QueueNetwork net, String eventFileName, String outFileName, double interval_s) {
		super(interval_s, new OTFQSimServerQuadBuilder(net), outFileName, new OTFFileWriterQSimConnectionManagerFactory());
		this.network = net;
		this.eventFileName = eventFileName;
	}

	@Override
	protected void onAdditionalQuadData(OTFConnectionManager connect) {
		this.quad.addAdditionalElement(this.writer);
	}

	private double lastTime=-1;

	public void convert() {
		open();

		// create SnapshotGenerator
		Gbl.getConfig().simulation().setSnapshotFormat("none");
		Gbl.getConfig().simulation().setSnapshotPeriod(this.interval_s);
		Events2Snapshot app = new Events2Snapshot();
		app.addExternalSnapshotWriter(this);
		app.run(new File(this.eventFileName), Gbl.getConfig(), this.network.getNetworkLayer());

		close();
	}

	@Override
	public void addAgent(PositionInfo position) {
		//drop all parking vehicles
		if (position.getAgentState() == AgentSnapshotInfo.AgentState.AGENT_AT_ACTIVITY) return;

		this.writer.positions.add(new OTFAgentsListHandler.ExtendedPositionInfo(position, 0,0));
	}

	@Override
	public void beginSnapshot(double time) {
		this.writer.positions.clear();
		this.lastTime = time;
	}

	@Override
	public void endSnapshot() {
		try {
			dump((int)this.lastTime);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		/* FIXME must this method really be overwritten? it does NOT call super.finish(), even that
		 * there is something done. So if this method is here to explicitly NOT call super.finish(), please
		 * add comment. Otherwise one might remove this method completely...
		 */
	}

}
