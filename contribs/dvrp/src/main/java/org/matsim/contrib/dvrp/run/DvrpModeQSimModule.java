/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.run;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentKeysRegistry;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpModeQSimModule extends AbstractQSimModule {
	private final String mode;
	private final boolean installPassengerEngineModule;
	private final Set<Class<? extends MobsimListener>> listeners;

	public DvrpModeQSimModule(String mode, boolean installPassengerEngineModule,
			Set<Class<? extends MobsimListener>> listeners) {
		this.mode = mode;
		this.installPassengerEngineModule = installPassengerEngineModule;
		this.listeners = listeners;
	}

	@Override
	protected void configureQSim() {
		install(new VrpAgentSourceQSimModule(mode));

		if (installPassengerEngineModule) {
			install(new PassengerEngineQSimModule(mode));
		}

		install(new AbstractMultiModeQSimModule(mode) {
			@Override
			protected void configureQSim() {
				listeners.stream().forEach(this::addModalComponent);
			}
		});
	}

	public void configureComponents( QSimComponentKeysRegistry components ) {
		components.addAnnotation(DvrpModes.mode(mode ) );
	}

	public static class Builder {
		private final String mode;
		private final Set<Class<? extends MobsimListener>> listeners = new HashSet<>();

		private boolean installPassengerEngineModule = true;

		public Builder(String mode) {
			this.mode = mode;
		}

		public Builder addListener(Class<? extends MobsimListener> listener) {
			if (!listeners.add(listener)) {
				throw new IllegalArgumentException("Listener: " + listener + " has been already added.");
			}
			return this;
		}

		public Builder addListeners(Collection<Class<? extends MobsimListener>> listener) {
			listener.forEach(this::addListener);
			return this;
		}

		public Builder setInstallPassengerEngineModule(boolean installPassengerEngineModule) {
			this.installPassengerEngineModule = installPassengerEngineModule;
			return this;
		}

		public DvrpModeQSimModule build() {
			return new DvrpModeQSimModule(mode, installPassengerEngineModule, listeners);
		}
	}
}
