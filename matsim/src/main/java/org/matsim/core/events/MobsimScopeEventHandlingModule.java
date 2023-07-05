/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.core.events;

import java.util.Set;

import jakarta.inject.Inject;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.multibindings.Multibinder;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MobsimScopeEventHandlingModule extends AbstractModule {
	@Override
	public void install() {
		bind(MobsimScopeEventHandling.class).asEagerSingleton();
		addControlerListenerBinding().to(MobsimScopeEventHandling.class);

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				Multibinder.newSetBinder(binder(), MobsimScopeEventHandler.class);
				bind(MobsimScopeEventHandlerRegistrator.class).asEagerSingleton();
			}
		});
	}

	static class MobsimScopeEventHandlerRegistrator {
		@Inject
		MobsimScopeEventHandlerRegistrator(MobsimScopeEventHandling eventHandling,
				Set<MobsimScopeEventHandler> eventHandlersDeclaredByModules) {
			for (MobsimScopeEventHandler eventHandler : eventHandlersDeclaredByModules) {
				eventHandling.addMobsimScopeHandler(eventHandler);
			}
		}
	}
}
