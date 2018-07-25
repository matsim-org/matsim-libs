/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.hybridsim.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class HybridMobsimProvider implements Provider<Mobsim>{
	
	private final Scenario sc;
	private final EventsManager em;

    @Inject
    HybridMobsimProvider(Scenario sc, EventsManager eventsManager) {
        this.sc = sc;
		this.em = eventsManager;

    }

	@Override
	public Mobsim get() {
		QSimConfigGroup conf = this.sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		
		Config config = this.sc.getConfig();
		
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		if (config.network().isTimeVariantNetwork()) {
		    plugins.add(new NetworkChangeEventsPlugin(config));
		}
		if (config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim() ) {
		    plugins.add(new TransitEnginePlugin(config));
		}
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new PopulationPlugin(config));
		plugins.add(new HybridQSimPlugin(config));
		

		List<AbstractModule> modules = Collections.singletonList(new AbstractModule() {
			@Override
			public void install() {
				QSimComponents components = new QSimComponents();
				new StandardQSimComponentsConfigurator(config).configure(components);
				
				components.activeMobsimEngines.add(HybridQSimPlugin.HYBRID_EXTERNAL_ENGINE);
				
				bind(QSimComponents.class).toInstance(components);
			}
		});
		
		return  QSimUtils.createQSim(sc, em, modules, plugins);
	}

}
