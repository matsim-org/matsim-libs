package org.matsim.contrib.hybridsim.run;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.hybridsim.simulation.HybridMobsimProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.HybridNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Created by laemmel on 18/02/16.
 */
public class RunHybridSim {

	public static void main(String [] args) {
		String config = null;
		if (args.length == 1) {
			config = args[0];
		} else {
			//TODO exit
		}
		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, config);
		c.controler().setWriteEventsInterval(1);

		final Scenario sc = ScenarioUtils.loadScenario(c);


		final Controler controller = new Controler(sc);
		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );


		final HybridNetworkFactory netFac = new HybridNetworkFactory();



		Injector mobsimProviderInjector = Guice.createInjector(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(Scenario.class).toInstance(sc);
				bind(EventsManager.class).toInstance(controller.getEvents());
				bind(HybridNetworkFactory.class).toInstance(netFac);
			}

		});
		final Provider<Mobsim> mobsimProvider = mobsimProviderInjector.getInstance(HybridMobsimProvider.class);
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Mobsim.class).toProvider(mobsimProvider);

			}
		});
		controller.run();
	}
}
