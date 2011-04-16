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

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import org.matsim.withinday.replanning.parallel.ParallelDuringLegReplanner;
import org.matsim.withinday.replanning.parallel.ParallelInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;

/**
 * This Class implements a SimulationBeforeSimStepListener.
 *
 * Each time a ListenerEvent is created it is checked
 * whether a WithinDayReplanning of the Agents Plans should
 * be done and / or is necessary.
 * 
 * @author: cdobler
 */
public class ReplanningManager implements SimulationBeforeSimStepListener, SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(ReplanningManager.class);
	
	private boolean initialReplanning = true;
	private boolean duringActivityReplanning = true;
	private boolean duringLegReplanning = true;

	private InitialReplanningModule initialReplanningModule;
	private DuringActivityReplanningModule duringActivityReplanningModule;
	private DuringLegReplanningModule duringLegReplanningModule;
	
	private ParallelInitialReplanner parallelInitialReplanner;
	private ParallelDuringActivityReplanner parallelDuringActivityReplanner;
	private ParallelDuringLegReplanner parallelDuringLegReplanner;
	
	public ReplanningManager(int numOfThreads) {
		initializeReplanningModules(numOfThreads);
	}
	
	private void initializeReplanningModules(int numOfThreads) {
		
		log.info("Initialize Parallel Replanning Modules");
		this.parallelInitialReplanner = new ParallelInitialReplanner(numOfThreads);
		this.parallelDuringActivityReplanner = new ParallelDuringActivityReplanner(numOfThreads);
		this.parallelDuringLegReplanner = new ParallelDuringLegReplanner(numOfThreads);

		log.info("Initialize Replanning Modules");
		initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		duringActivityReplanningModule = new DuringActivityReplanningModule(parallelDuringActivityReplanner);
		duringLegReplanningModule = new DuringLegReplanningModule(parallelDuringLegReplanner);

		this.setInitialReplanningModule(initialReplanningModule);
		this.setDuringActivityReplanningModule(duringActivityReplanningModule);
		this.setDuringLegReplanningModule(duringLegReplanningModule);
	}
	
	public void doInitialReplanning(boolean value) {
		initialReplanning = value;
	}

	public boolean isInitialReplanning() {
		return initialReplanning;
	}

	public void setInitialReplanningModule(InitialReplanningModule module) {
		this.initialReplanningModule = module;
	}

	public InitialReplanningModule getInitialReplanningModule() {
		return this.initialReplanningModule;
	}

	public void doDuringActivityReplanning(boolean value) {
		duringActivityReplanning = value;
	}

	public boolean isDuringActivityReplanning() {
		return duringActivityReplanning;
	}

	public void setDuringActivityReplanningModule(DuringActivityReplanningModule module) {
		this.duringActivityReplanningModule = module;
	}

	public DuringActivityReplanningModule getDuringActivityReplanningModule() {
		return this.duringActivityReplanningModule;
	}

	public void doDuringLegReplanning(boolean value) {
		duringLegReplanning = value;
	}

	public boolean isDuringLegReplanning() {
		return duringLegReplanning;
	}

	public void setDuringLegReplanningModule(DuringLegReplanningModule module) {
		this.duringLegReplanningModule = module;
	}

	public DuringLegReplanningModule getDuringLegReplanningModule() {
		return this.duringLegReplanningModule;
	}

	public void addIntialReplanner(WithinDayInitialReplanner replanner) {
		this.parallelInitialReplanner.addWithinDayReplanner(replanner);
	}
	
	public void addDuringActivityReplanner(WithinDayDuringActivityReplanner replanner) {		
		this.parallelDuringActivityReplanner.addWithinDayReplanner(replanner);
	}
	
	public void addDuringLegReplanner(WithinDayDuringLegReplanner replanner) {		
		this.parallelDuringLegReplanner.addWithinDayReplanner(replanner);
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		if (isInitialReplanning()) {
			initialReplanningModule.doReplanning(Time.UNDEFINED_TIME);
		}
	}

	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		if (isDuringActivityReplanning()) {
			duringActivityReplanningModule.doReplanning(e.getSimulationTime());
		}

		if (isDuringLegReplanning()) {
			duringLegReplanningModule.doReplanning(e.getSimulationTime());
		}
	}
}