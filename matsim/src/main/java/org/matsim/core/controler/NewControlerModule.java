
/* *********************************************************************** *
 * project: org.matsim.*
 * NewControlerModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import com.google.inject.multibindings.Multibinder;
import org.matsim.analysis.IterationStopWatch;

public final class NewControlerModule extends AbstractModule {
	@Override
	public void install() {
		bind(ControlerI.class).to(NewControler.class).asEagerSingleton();
		bind(ControlerListenerManagerImpl.class).asEagerSingleton();
		bind(ControlerListenerManager.class).to(ControlerListenerManagerImpl.class);

		bind(IterationStopWatch.class).asEagerSingleton();
		bind(OutputDirectoryHierarchy.class).asEagerSingleton();
		bind(TerminationCriterion.class).to(TerminateAtFixedIterationNumber.class);
		bind(MatsimServices.class).to(MatsimServicesImpl.class);

		bind(IterationCounter.class).to(MatsimServicesImpl.class);
		// (I don't want to always inject the whole MatsimServices just to get the iteration number.  If
		// someone has a better idea, please let me know. kai, aug'18)

		bind(PrepareForSim.class).to(PrepareForSimImpl.class);
		bind(PrepareForMobsim.class).to(PrepareForMobsimImpl.class);

		// Explicitly create the set binder so that it always available (even if empty)
		Multibinder.newSetBinder(binder(), PersonPrepareForSimAlgorithm.class);
	}
}
