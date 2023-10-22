/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.controler;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.corelisteners.*;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.Mobsim;

import jakarta.inject.Inject;
import java.util.Set;

class NewControler extends AbstractController implements ControlerI {

	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(NewControler.class);

	private final Config config;
	private final PrepareForSim prepareForSim;
	private final PrepareForMobsim prepareForMobsim;
	private final EventsHandling eventsHandling;
	private final PlansDumping plansDumping;
	private final PlansReplanning plansReplanning;
	private final Provider<Mobsim> mobsimProvider;
	private final PlansScoring plansScoring;
	private final TerminationCriterion terminationCriterion;
	private final DumpDataAtEnd dumpDataAtEnd;
	private final Set<ControlerListener> controlerListenersDeclaredByModules;
	private final ControllerConfigGroup controllerConfigGroup;
	private final OutputDirectoryHierarchy outputDirectoryHierarchy;

	@Inject
	NewControler(Config config, ControlerListenerManagerImpl controlerListenerManager, MatsimServices matsimServices,
			 IterationStopWatch stopWatch, PrepareForSim prepareForSim, EventsHandling eventsHandling,
			 PlansDumping plansDumping, PlansReplanning plansReplanning, Provider<Mobsim> mobsimProvider,
			 PlansScoring plansScoring, TerminationCriterion terminationCriterion, DumpDataAtEnd dumpDataAtEnd,
			 Set<ControlerListener> controlerListenersDeclaredByModules, ControllerConfigGroup controllerConfigGroup,
			 OutputDirectoryHierarchy outputDirectoryHierarchy
			, PrepareForMobsim prepareForMobsim
 ) {
		super(controlerListenerManager, stopWatch, matsimServices);
		this.config = config;
		this.prepareForMobsim = prepareForMobsim;
		this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		this.prepareForSim = prepareForSim;
		this.eventsHandling = eventsHandling;
		this.plansDumping = plansDumping;
		this.plansReplanning = plansReplanning;
		this.mobsimProvider = mobsimProvider;
		this.plansScoring = plansScoring;
		this.terminationCriterion = terminationCriterion;
		this.dumpDataAtEnd = dumpDataAtEnd;
		this.controlerListenersDeclaredByModules = controlerListenersDeclaredByModules;
		this.controllerConfigGroup = controllerConfigGroup;
		this.outputDirectoryHierarchy = outputDirectoryHierarchy;
	}

	@Override
	public final void run() {
		super.setupOutputDirectory(outputDirectoryHierarchy);
		super.run(this.config);
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	@Override
	protected final void loadCoreListeners() {
		/*
		 * The order how the listeners are added is very important! As
		 * dependencies between different listeners exist or listeners may read
		 * and write to common variables, the order is important.
		 *
		 * IMPORTANT: The execution order is reverse to the order the listeners
		 * are added to the list.
		 */
		if (controllerConfigGroup.getDumpDataAtEnd()) {
			this.addCoreControlerListener(this.dumpDataAtEnd);
		}

		this.addCoreControlerListener(this.plansScoring);
		this.addCoreControlerListener(this.plansReplanning);
		this.addCoreControlerListener(this.plansDumping);
		this.addCoreControlerListener(this.eventsHandling);
		// must be last being added (=first being executed)

		for (ControlerListener controlerListener : this.controlerListenersDeclaredByModules) {
			this.addControlerListener(controlerListener);
		}
	}

	@Override
	protected final void prepareForSim() {
		this.prepareForSim.run();
	}

	@Override
	protected final void prepareForMobsim() {
		this.prepareForMobsim.run() ;
//		this.prepareForSim.run() ;
	}

	@Override
	protected final void runMobSim() {
		this.mobsimProvider.get().run();
	}

	@Override
	protected final boolean mayTerminateAfterIteration(int iteration) {
		return terminationCriterion.mayTerminateAfterIteration(iteration);
	}

	@Override
	protected final boolean shouldTerminate(int iteration) {
		return terminationCriterion.doTerminate(iteration);
	}
}
