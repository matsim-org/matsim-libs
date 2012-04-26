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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import org.matsim.withinday.replanning.parallel.ParallelDuringLegReplanner;
import org.matsim.withinday.replanning.parallel.ParallelInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

/**
 * This Class implements a SimulationBeforeSimStepListener.
 *
 * Each time a ListenerEvent is created it is checked
 * whether a WithinDayReplanning of the Agents Plans should
 * be done and / or is necessary.
 * 
 * @author: cdobler
 */
public class ReplanningManager implements MobsimEngine, MobsimBeforeSimStepListener {

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
	
	private Map<WithinDayDuringActivityReplannerFactory, Tuple<Double, Double>> duringActivityReplannerFactory;
	private Map<WithinDayDuringLegReplannerFactory, Tuple<Double, Double>> duringLegReplannerFactory;
	
	private InternalInterface internalInterface;
	
	public ReplanningManager() {
		duringActivityReplannerFactory = new LinkedHashMap<WithinDayDuringActivityReplannerFactory, Tuple<Double, Double>>();
		duringLegReplannerFactory = new LinkedHashMap<WithinDayDuringLegReplannerFactory, Tuple<Double, Double>>();
	}
	
	/*
	 * TODO: Create a config group and get number of threads from there.
	 */
	public void initializeReplanningModules(int numOfThreads) {
		
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
	
	public void setEventsManager(EventsManager eventsManager) {
		this.parallelInitialReplanner.setEventsManager(eventsManager);
		this.parallelDuringActivityReplanner.setEventsManager(eventsManager);
		this.parallelDuringLegReplanner.setEventsManager(eventsManager);
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
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		double time = e.getSimulationTime();
		
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
	public void doSimStep(double time) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPrepareSim() {
		if (isInitialReplanning()) {
			initialReplanningModule.doReplanning(Time.UNDEFINED_TIME);
		}
		
		// reset all replanners
		parallelInitialReplanner.resetReplanners();
		parallelDuringActivityReplanner.resetReplanners();
		parallelDuringLegReplanner.resetReplanners();
	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub	
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
	
	public InternalInterface getInternalInterface() {
		return this.internalInterface;
	}
}