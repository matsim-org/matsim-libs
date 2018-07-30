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
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;

import com.google.inject.Provides;

public class DynQSimModule extends AbstractModule {
	@Override
	public void install() {
	}

	@Provides
	public Collection<AbstractQSimPlugin> provideDynQSimPlugins(Config config) {
		return provideQSimPlugins(config);
	}

	public static Collection<AbstractQSimPlugin> provideQSimPlugins(Config config) {
		//use the standard plugins, but replace ActivityEnginePlugin with DynActivityEnginePlugin
		return QSimModule.getDefaultQSimPlugins(config)
				.stream()
				.map(p -> p instanceof ActivityEnginePlugin ? new DynActivityEnginePlugin(config) : p)
				.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
	}

	@Provides
	private QSimComponents provideQSimComponents(Config config) {
		QSimComponents components = new QSimComponents();
		new StandardQSimComponentsConfigurator(config).configure(components);
		new DynAgentQSimComponentsConfigurator().configure(components);
		return components;
	}
}
