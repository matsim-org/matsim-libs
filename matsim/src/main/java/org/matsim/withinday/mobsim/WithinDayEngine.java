/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.withinday.mobsim;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.ActivityEndReschedulerProvider;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import org.matsim.withinday.replanning.parallel.ParallelDuringLegReplanner;
import org.matsim.withinday.replanning.parallel.ParallelInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

/**
 * This Class implements the MobsimEngine interface. If added to a
 * QSim, the QSim's internals ensure that WithinDayEngine.doSimStep(...)
 * is performed before the other MobsimEngines.
 *
 * Also, it lives as long as the Controler, as opposed to all other parts of the QSim,
 * and is added to each new QSim instance in turn.
 *
 * @author cdobler
 */
@Singleton
public class WithinDayEngine implements MobsimEngine, ActivityEndReschedulerProvider {

	private static final Logger log = LogManager.getLogger(WithinDayEngine.class);

	private final EventsManager eventsManager;

	private boolean initialReplanning = true;
	private boolean duringActivityReplanning = true;
	private boolean duringLegReplanning = true;

	private boolean initialReplanningPerformed = false;

	private InitialReplanningModule initialReplanningModule;
	private DuringActivityReplanningModule duringActivityReplanningModule;
	private DuringLegReplanningModule duringLegReplanningModule;

	private ParallelInitialReplanner parallelInitialReplanner;
	private ParallelDuringActivityReplanner parallelDuringActivityReplanner;
	private ParallelDuringLegReplanner parallelDuringLegReplanner;

	private Map<WithinDayDuringActivityReplannerFactory, Tuple<Double, Double>> duringActivityReplannerFactory;
	private Map<WithinDayDuringLegReplannerFactory, Tuple<Double, Double>> duringLegReplannerFactory;

	private InternalInterface internalInterface;

	@Inject
	public WithinDayEngine(EventsManager eventsManager, GlobalConfigGroup globalConfigGroup) {
		this.eventsManager = eventsManager;

		this.duringActivityReplannerFactory = new LinkedHashMap<>();
		this.duringLegReplannerFactory = new LinkedHashMap<>();

		log.info("Initialize Parallel Replanning Modules");
		this.parallelInitialReplanner = new ParallelInitialReplanner(globalConfigGroup.getNumberOfThreads(), eventsManager);
		this.parallelDuringActivityReplanner = new ParallelDuringActivityReplanner(globalConfigGroup.getNumberOfThreads(), eventsManager);
		this.parallelDuringLegReplanner = new ParallelDuringLegReplanner(globalConfigGroup.getNumberOfThreads(), eventsManager);

		log.info("Initialize Replanning Modules");
		this.initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		this.duringActivityReplanningModule = new DuringActivityReplanningModule(parallelDuringActivityReplanner);
		this.duringLegReplanningModule = new DuringLegReplanningModule(parallelDuringLegReplanner);
	}

	public void doInitialReplanning(boolean value) {
		initialReplanning = value;
	}

	public boolean isInitialReplanning() {
		return initialReplanning;
	}

	public void doDuringActivityReplanning(boolean value) {
		duringActivityReplanning = value;
	}

	public boolean isDuringActivityReplanning() {
		return duringActivityReplanning;
	}

	public void doDuringLegReplanning(boolean value) {
		duringLegReplanning = value;
	}

	public boolean isDuringLegReplanning() {
		return duringLegReplanning;
	}

	public void addIntialReplannerFactory(WithinDayInitialReplannerFactory factory) {
		this.parallelInitialReplanner.addWithinDayReplannerFactory(factory);
	}

	public void addDuringActivityReplannerFactory(WithinDayDuringActivityReplannerFactory factory) {
		this.parallelDuringActivityReplanner.addWithinDayReplannerFactory(factory);
	}

	public void addDuringLegReplannerFactory(WithinDayDuringLegReplannerFactory factory) {
		this.parallelDuringLegReplanner.addWithinDayReplannerFactory(factory);
	}

	public void removeInitialReplannerFactory(WithinDayInitialReplannerFactory factory) {
		this.parallelInitialReplanner.removeWithinDayReplannerFactory(factory);
	}

	public void removeDuringActivityReplannerFactory(WithinDayDuringActivityReplannerFactory factory) {
		this.parallelDuringActivityReplanner.removeWithinDayReplannerFactory(factory);
	}

	public void removeDuringLegReplannerFactory(WithinDayDuringLegReplannerFactory factory) {
		this.parallelDuringLegReplanner.removeWithinDayReplannerFactory(factory);
	}

	public void addTimedDuringActivityReplannerFactory(WithinDayDuringActivityReplannerFactory factory, double startReplanning, double endReplanning) {
		Tuple<Double, Double> tuple = new Tuple<Double, Double>(startReplanning, endReplanning);
		this.duringActivityReplannerFactory.put(factory, tuple);
	}

	public void addTimedDuringLegReplannerFactory(WithinDayDuringLegReplannerFactory factory, double startReplanning, double endReplanning) {
		Tuple<Double, Double> tuple = new Tuple<Double, Double>(startReplanning, endReplanning);
		this.duringLegReplannerFactory.put(factory, tuple);
	}

	@Override
	public void doSimStep(double time) {

		/*
		 * Initial replanning (so far?) cannot be performed in the onPrepareSim()
		 * method since the identifiers and replanners do not know the agents at
		 * that point in time.
		 */
		if (!initialReplanningPerformed && isInitialReplanning()) {
			initialReplanningModule.doReplanning(Double.NEGATIVE_INFINITY);//-Inf == before any feasible time step
			initialReplanningPerformed = true;
		}

		for (Entry<WithinDayDuringActivityReplannerFactory, Tuple<Double, Double>> entry : duringActivityReplannerFactory.entrySet()) {
			if (entry.getValue().getFirst() == time) this.parallelDuringActivityReplanner.addWithinDayReplannerFactory(entry.getKey());
			if (entry.getValue().getSecond() == time) this.parallelDuringActivityReplanner.removeWithinDayReplannerFactory(entry.getKey());
		}
		for (Entry<WithinDayDuringLegReplannerFactory, Tuple<Double, Double>> entry : duringLegReplannerFactory.entrySet()) {
			if (entry.getValue().getFirst() == time) this.parallelDuringLegReplanner.addWithinDayReplannerFactory(entry.getKey());
			if (entry.getValue().getSecond() == time) this.parallelDuringLegReplanner.removeWithinDayReplannerFactory(entry.getKey());
		}

		if (isDuringActivityReplanning()) {
			duringActivityReplanningModule.doReplanning(time);
		}

		if (isDuringLegReplanning()) {
			duringLegReplanningModule.doReplanning(time);
		}
	}

	@Override
	public void onPrepareSim() {
		this.parallelInitialReplanner.onPrepareSim();
		this.parallelDuringActivityReplanner.onPrepareSim();
		this.parallelDuringLegReplanner.onPrepareSim();

		// reset all replanners
		this.parallelInitialReplanner.resetReplanners();
		this.parallelDuringActivityReplanner.resetReplanners();
		this.parallelDuringLegReplanner.resetReplanners();

		this.initialReplanningPerformed = false;
	}

	@Override
	public void afterSim() {
		this.parallelInitialReplanner.afterSim();
		this.parallelDuringActivityReplanner.afterSim();
		this.parallelDuringLegReplanner.afterSim();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}


	@Override
	public ActivityEndRescheduler getActivityRescheduler() {
		return this.internalInterface.getMobsim();
	}
}
