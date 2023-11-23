/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SnapshotWritersModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.vis.snapshotwriters;

import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.ReplanningContext;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.Collection;

import static org.matsim.core.config.groups.ControllerConfigGroup.SnapshotFormat;

public class SnapshotWritersModule extends AbstractModule {

	public static final String GENERATE_SNAPSHOT_FOR_LINK_KEY = "generateSnapshotForLink";

	@Override
	public void install() {
		if (getConfig().controller().getSnapshotFormat().contains(SnapshotFormat.transims)) {
			addSnapshotWriterBinding().toProvider(TransimsSnapshotWriterFactory.class);
		}
		if (getConfig().controller().getSnapshotFormat().contains(SnapshotFormat.positionevents)) {
			addSnapshotWriterBinding().toProvider(PositionEventsWriterFactory.class);
		}
		if (getConfig().controller().getWriteSnapshotsInterval() != 0) {
			addMobsimListenerBinding().toProvider(SnapshotWriterManagerProvider.class);
		}
	}

	private static class SnapshotWriterManagerProvider implements Provider<MobsimListener> {

		private final QSimConfigGroup qSimConfigGroup;
		private final ControllerConfigGroup controllerConfigGroup;
		private final ReplanningContext iterationContext;
		private final Collection<com.google.inject.Provider<SnapshotWriter>> snapshotWriters;

		@Inject
		private SnapshotWriterManagerProvider(QSimConfigGroup qSimConfigGroup, ControllerConfigGroup controllerConfigGroup,
											  ReplanningContext iterationContext,
											  Collection<com.google.inject.Provider<SnapshotWriter>> snapshotWriters) {
			this.qSimConfigGroup = qSimConfigGroup;
			this.controllerConfigGroup = controllerConfigGroup;
			this.iterationContext = iterationContext;
			this.snapshotWriters = snapshotWriters;
		}

		@Override
		public MobsimListener get() {
			if (iterationContext.getIteration() % controllerConfigGroup.getWriteSnapshotsInterval() == 0) {
				SnapshotWriterManager manager = new SnapshotWriterManager((int) qSimConfigGroup.getSnapshotPeriod(), qSimConfigGroup.getFilterSnapshots());
				for (com.google.inject.Provider<SnapshotWriter> snapshotWriter : this.snapshotWriters) {
					manager.addSnapshotWriter(snapshotWriter.get());
				}
				return manager;
			} else {
				return new NoopMobsimListener();
			}
		}

		private static class NoopMobsimListener implements MobsimListener {
		}
	}}
