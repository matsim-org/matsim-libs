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

package org.matsim.utils.vis.otfvis.executables;

import java.io.File;
import java.io.IOException;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.run.Events2Snapshot;
import org.matsim.utils.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.utils.vis.otfvis.server.OTFQuadFileHandler;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.world.World;

public class OTFEvent2MVI extends OTFQuadFileHandler.Writer {
	private final String eventFileName;

	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();

	public OTFEvent2MVI(QueueNetwork net, String eventFileName, String outFileName, double interval_s) {
		super(interval_s, net, outFileName);
		this.eventFileName = eventFileName;
	}

	@Override
	protected void onAdditionalQuadData() {
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
		app.run(new File(this.eventFileName), Gbl.getConfig(), this.net.getNetworkLayer());

		close();
	}

	public static void main(String[] args) {

		if ( args.length==0 )
			args = new String[] {"../../tmp/studies/berlin-wip/config_ds.xml"};

		Gbl.createConfig(args);
		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		String netFileName = Gbl.getConfig().getParam("network","inputNetworkFile");
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);
		QueueNetwork qnet = new QueueNetwork(net);

		String eventFile = null;
		eventFile = "output/current/ITERS/it.0/0.events.txt.gz";
		eventFile = "../../tmp/studies/berlin-wip/run125/200.events.txt.gz";

		OTFEvent2MVI test  = new OTFEvent2MVI(qnet, eventFile, "output/ds_fromEvent.mvi",600);
		test.convert();
	}

	@Override
	public void addAgent(PositionInfo position) {
		//drop all parking vehicles
		if (position.getVehicleState() == PositionInfo.VehicleState.Parking) return;

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
		 * there is something done. So if this method is here to explicitely NOT call super.finish(), please
		 * add comment. Otherwise one might remove this method completely...
		 */
	}

}
