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
import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpModeQSimModule extends AbstractQSimModule {
	private final String mode;
	private final boolean installPassengerEngineModule;
	private final Map<String, Class<? extends MobsimListener>> listeners;

	public DvrpModeQSimModule(String mode, boolean installPassengerEngineModule,
			Map<String, Class<? extends MobsimListener>> listeners) {
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

		install(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				for (Map.Entry<String, Class<? extends MobsimListener>> entry : listeners.entrySet()) {
					addNamedComponent(entry.getValue(), entry.getKey());
				}
			}
		});
	}

	public void configureComponents(QSimComponents components) {
		VrpAgentSourceQSimModule.configureComponents(components, mode);

		if (installPassengerEngineModule) {
			PassengerEngineQSimModule.configureComponents(components, mode);
		}
		
		listeners.keySet().forEach(components::addNamedComponent);
	}

	public static class Builder {
		private final String mode;
		private final Map<String, Class<? extends MobsimListener>> listeners = new HashMap<>();

		private int listenerIndex = 0;
		private boolean installPassengerEngineModule = true;

		public Builder(String mode) {
			this.mode = mode;
		}

		private String createNextListenerName(Class<? extends MobsimListener> listener) {
			return String.format("DVRP_%s_%d_%s", mode, listenerIndex++, listener.getClass().toString());
		}

		public Builder addListener(Class<? extends MobsimListener> listener) {
			listeners.put(createNextListenerName(listener), listener);
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
