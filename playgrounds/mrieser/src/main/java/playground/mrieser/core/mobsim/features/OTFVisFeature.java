/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.features;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;

import playground.mrieser.core.mobsim.network.api.VisLink;
import playground.mrieser.core.mobsim.network.api.VisNetwork;

public class OTFVisFeature implements MobsimFeature {

	private final VisNetwork visNetwork;
	private final SnapshotWriter snapshotWriter;
	private boolean takeSnapshots = true;

	public OTFVisFeature(final VisNetwork visNetwork, final SnapshotWriter snapshotWriter) {
		this.visNetwork = visNetwork;
		this.snapshotWriter = snapshotWriter;
	}

	@Override
	public void beforeMobSim() {
	}

	@Override
	public void doSimStep(double time) {
		if (time > 0) {
			Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>(10000);
			for (VisLink link : visNetwork.getLinks().values()) {
				link.getVehiclePositions(positions);
			}
			this.snapshotWriter.beginSnapshot(time);
			for (AgentSnapshotInfo pi : positions) {
				this.snapshotWriter.addAgent(pi);
			}
			this.snapshotWriter.endSnapshot();
		}
	}

	@Override
	public void afterMobSim() {
	}

}
