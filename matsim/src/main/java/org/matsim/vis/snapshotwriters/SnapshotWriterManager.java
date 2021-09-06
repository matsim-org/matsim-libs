/* *********************************************************************** *
 * project: org.matsim.*
 * SnapshotWriterManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SnapshotWriterManager implements MobsimBeforeCleanupListener, MobsimAfterSimStepListener, MobsimInitializedListener {

	private final List<SnapshotWriter> snapshotWriters = new ArrayList<>();
	private final QSimConfigGroup.FilterSnapshots filterSnapshots;
	private final int snapshotPeriod;

	/**
	 * time since last snapshot
	 */
	private double snapshotTime = 0.0;

	public SnapshotWriterManager(int snapshotPeriod, QSimConfigGroup.FilterSnapshots filterSnapshots) {
		this.snapshotPeriod = snapshotPeriod;
		this.filterSnapshots = filterSnapshots;
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		Netsim mobsim = (Netsim) e.getQueueSimulation();
		this.snapshotTime = Math.floor(mobsim.getSimTimer().getSimStartTime()
				/ this.snapshotPeriod)
				* this.snapshotPeriod;
		if (this.snapshotTime < mobsim.getSimTimer().getSimStartTime()) {
			this.snapshotTime += this.snapshotPeriod;
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		closeSnapshotWriters();
	}

	private void closeSnapshotWriters() {
		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		double time = e.getSimulationTime();
		if (time >= this.snapshotTime) {
			this.snapshotTime += this.snapshotPeriod;
			doSnapshot(time, (VisMobsim) e.getQueueSimulation());
		}
	}
	
	private void doSnapshot(final double time, VisMobsim visMobsim) {
		if (!this.snapshotWriters.isEmpty()) {

			// apparently not paralizable
			var positions = visMobsim.getVisNetwork().getVisLinks().values().stream()
					.filter(visLink -> isGenerateSnapshot(visLink.getLink()))
					.flatMap(visLink -> visLink.getVisData().addAgentSnapshotInfo(new ArrayList<>()).stream())
					.collect(Collectors.toList());

			// We do not put non-network agents in movies.
			// Otherwise, we would add snapshots from visMobsim.getNonNetworkAgentSnapshots() here.

			for (SnapshotWriter writer : this.snapshotWriters) {
				writer.beginSnapshot(time);
				for (AgentSnapshotInfo position : positions) {
					writer.addAgent(position);
				}
				writer.endSnapshot();
			}
		}
	}

	public final void addSnapshotWriter(SnapshotWriter snapshotWriter) {
		this.snapshotWriters.add(snapshotWriter);
	}

	private boolean isGenerateSnapshot(Link link) {
		switch (filterSnapshots) {
			case no:
				return true;
			case withLinkAttributes:
				return link.getAttributes().getAttribute(SnapshotWritersModule.GENERATE_SNAPSHOT_FOR_LINK_KEY) != null;
			default:
				throw new RuntimeException("Unexpected filter snapshot setting: " + filterSnapshots + " Possible are: [no, withLinkAttributes]. This can be changed in config.qsim.filterSnapshots");
		}
	}
}
