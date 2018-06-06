/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * WithinDayTransitModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.pt.withinday;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;

import com.google.inject.Inject;
import com.google.inject.Provides;

public class WithinDayTransitModule extends AbstractModule {
	
	private Config config;

	@Inject
	public WithinDayTransitModule(Config config) {
		this.config = config;
	}
	
	@Override
	public void install() {
		bind(WithinDayTransitEngine.class);
        bind(Mobsim.class).toProvider(WithinDayTransitQSimFactory.class);
	}
	
	@Provides
	public Collection<AbstractQSimPlugin> provideQSimPlugins() {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		if (config.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}
		if (config.transit().isUseTransit()) {
			plugins.add(new TransitEnginePlugin(config));
		}
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new WithinDayTransitPopulationPlugin(config));
		return plugins;
	}

}
