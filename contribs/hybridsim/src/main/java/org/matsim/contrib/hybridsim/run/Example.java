/****************************************************************************/
// SUMO, Simulation of Urban MObility; see http://sumo.dlr.de/
// Copyright (C) 2001-2016 DLR (http://www.dlr.de/) and contributors
/****************************************************************************/
//
//   This file is part of SUMO.
//   SUMO is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
/****************************************************************************/

package org.matsim.contrib.hybridsim.run;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.hybridsim.simulation.HybridMobsimProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.HybridNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by laemmel on 09.03.16.
 */
public class Example {

	private static final Logger log = Logger.getLogger(Example.class);

	public static void main(String [] args) throws IOException {

		ExternalRunner r = new ExternalRunner();
		Thread t1 = new Thread(r);
		t1.start();

		Config c = ConfigUtils.createConfig();
		c.network().setInputFile("./src/main/resources/network.xml");
		c.plans().setInputFile("./src/main/resources/population.xml");

		final Scenario sc = ScenarioUtils.loadScenario(c);
		final Controler controller = new Controler(sc);
		controller.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		final HybridNetworkFactory netFac = new HybridNetworkFactory();

		final EventsManager eventsManager = EventsUtils.createEventsManager();

		Injector mobsimProviderInjector = Guice.createInjector(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(Scenario.class).toInstance(sc);
				bind(EventsManager.class).toInstance(eventsManager);
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

	private static final class ExternalRunner implements Runnable {

		@Override
		public void run() {
			Process process = null;
			try {
				process = new ProcessBuilder("/usr/local/bin/jpscore","./src/main/resources/corridor_ini.xml").start();
				logToLog(process);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void logToLog(Process p1) throws IOException {
		{
			InputStream is = p1.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String l = br.readLine();
			while (l != null) {
				log.info(l);
				l = br.readLine();
			}
		}
		{
			InputStream is = p1.getErrorStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String l = br.readLine();
			while (l != null) {
				log.error(l);
				l = br.readLine();
			}
		}
	}
}
