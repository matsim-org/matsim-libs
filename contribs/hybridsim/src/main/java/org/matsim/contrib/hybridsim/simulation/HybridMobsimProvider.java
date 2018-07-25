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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;

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
		
		return new QSimBuilder(config) //
				.useDefaults() //
				.addPlugin(new HybridQSimPlugin(config)) //
				.configureComponents(components -> {
					components.activeMobsimEngines.add(HybridQSimPlugin.HYBRID_EXTERNAL_ENGINE);
				}) //
				.build(sc, em);
	}

}
