/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.DeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.SimpleDeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SimpleDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.TrainDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import com.google.inject.multibindings.OptionalBinder;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentFactory;

/**
 * Module to install railsim functionality.
 */
public class RailsimQSimModule extends AbstractQSimModule implements QSimComponentsConfigurator {

	public static final String COMPONENT_NAME = "Railsim";

	@Override
	public void configure(QSimComponentsConfig components) {
		components.addNamedComponent(COMPONENT_NAME);
	}

	@Override
	protected void configureQSim() {
		bind(RailsimQSimEngine.class).asEagerSingleton();

		bind(TrainRouter.class).asEagerSingleton();
		bind(RailResourceManager.class).asEagerSingleton();

		// These interfaces might be replaced with other implementations
		bind(TrainDisposition.class).to(SimpleDisposition.class).asEagerSingleton();
		bind(DeadlockAvoidance.class).to(SimpleDeadlockAvoidance.class).asEagerSingleton();

		addQSimComponentBinding(COMPONENT_NAME).to(RailsimQSimEngine.class);

		OptionalBinder.newOptionalBinder(binder(), TransitDriverAgentFactory.class)
			.setBinding().to(RailsimDriverAgentFactory.class);
	}
}
