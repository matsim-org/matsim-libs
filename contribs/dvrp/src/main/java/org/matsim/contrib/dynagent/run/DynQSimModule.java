/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent.run;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;

import com.google.inject.Provides;

public class DynQSimModule extends AbstractModule {
	private final Class<? extends AgentSource> agentSourceClass;

	public DynQSimModule(Class<? extends AgentSource> agentSourceClass) {
		this.agentSourceClass = agentSourceClass;
	}

	@Override
	public void install() {
	}

	@Provides
	public Collection<AbstractQSimModule> provideDynQSimPlugins(Config config) {
		return provideQSimPlugins(config, agentSourceClass);
	}

	public static Collection<AbstractQSimModule> provideQSimPlugins(Config config,
			Class<? extends AgentSource> agentSourceClass) {
		//use the standard plugins, but replace ActivityEnginePlugin with DynActivityEnginePlugin
		Collection<AbstractQSimModule> plugins = QSimModule.getDefaultQSimModules()
				.stream()
				.map(p -> p instanceof ActivityEngineModule ? new DynActivityEngineModule() : p)
				.collect(Collectors.toList());
		plugins.add(new DynAgentSourceModule(agentSourceClass));
		return Collections.unmodifiableCollection(plugins);
	}

	@Provides
	private QSimComponents provideQSimComponents(Config config) {
		QSimComponents components = new QSimComponents();
		new StandardQSimComponentsConfigurator(config).configure(components);
		new DynQSimComponentsConfigurator().configure(components);
		return components;
	}
}
