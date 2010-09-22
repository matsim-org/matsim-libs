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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.util.Collection;

import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfoFactory;

import playground.mrieser.core.mobsim.api.SimVehicle;
import playground.mrieser.core.mobsim.network.api.VisLink;

public class VisLinkImpl implements VisLink {

	private final QueueLink link;

	public VisLinkImpl(final QueueLink link) {
		this.link = link;
	}

	@Override
	public void getVehiclePositions(final Collection<AgentSnapshotInfo> positions) {

		double dist = this.link.link.getLength();
		int vehCount = this.link.buffer.buffer.size() + this.link.vehQueue.size();
		double vehSize = Math.min(7.5, dist / vehCount);

		for (SimVehicle veh : this.link.buffer.buffer) {
			dist -= vehSize;
			AgentSnapshotInfo pi = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(veh.getId(), this.link.link, dist, 1);
			pi.setColorValueBetweenZeroAndOne(1.0);
			pi.setAgentState(AgentState.PERSON_DRIVING_CAR);
			positions.add(pi);
		}
		vehSize = dist / this.link.vehQueue.size();
		for (SimVehicle veh : this.link.vehQueue) {
			dist -= vehSize;
			AgentSnapshotInfo pi = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(veh.getId(), this.link.link, dist, 1);
			pi.setColorValueBetweenZeroAndOne(0.5);
			pi.setAgentState(AgentState.PERSON_DRIVING_CAR);
			positions.add(pi);
		}
	}

}
