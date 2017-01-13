/* *********************************************************************** *
 * project: org.matsim.*
 * LaemmerOTFStarter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.laemmer.run;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.dgrether.signalsystems.laemmer.model.LaemmerSignalsModule;
import playground.dgrether.utils.DgOTFVisUtils;

/**
 * @author dgrether
 *
 */
public class LaemmerOTFStarter {

	public void prepare4SimAndPlay(Scenario scenario) {
		DgOTFVisUtils.preparePopulation4Simulation(scenario);
		this.playScenario(scenario);
	}

	public void playScenario(Scenario scenario) {

		Collection<AbstractModule> defaultsModules = new ArrayList<>();
		defaultsModules.add(new ScenarioByInstanceModule(scenario));
		defaultsModules.add(new ControlerDefaultsModule());

		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
				install(new LaemmerSignalsModule());
			}
		});

		EventsManager events = injector.getInstance(EventsManager.class);
		events.initProcessing();
		
		QSim otfVisQSim = (QSim) injector.getInstance(Mobsim.class);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		
		otfVisQSim.run();

	}

}
