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
package signals.laemmer.run;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.signals.otfvis.OTFClientLiveWithSignals;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.vis.otfvis.OnTheFlyServer;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.util.Types;

import playground.dgrether.utils.DgOTFVisUtils;
import signals.CombinedSignalsModule;

/**
 * @author dgrether
 * @author tthunig
 *
 */
public class LaemmerOTFStarter {

	public void prepare4SimAndPlay(Scenario scenario) {
		DgOTFVisUtils.preparePopulation4Simulation(scenario);
		this.playScenario(scenario);
	}

	public void playScenario(Scenario scenario) {

		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				// defaults
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
				
				// signal specific module
//				install(new LaemmerSignalsModule());
				install(new CombinedSignalsModule());
			}
		});

		EventsManager events = injector.getInstance(EventsManager.class);
		events.initProcessing();
		
		QSim otfVisQSim = (QSim) injector.getInstance(Mobsim.class);
		Collection<Provider<MobsimListener>> mobsimListeners = (Collection<Provider<MobsimListener>>) injector.getInstance(Key.get(Types.collectionOf(Types.providerOf(MobsimListener.class))));
		for (Provider<MobsimListener> provider : mobsimListeners) {
			otfVisQSim.addQueueSimulationListeners(provider.get());
		}
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLiveWithSignals.run(scenario.getConfig(), server);
		
		otfVisQSim.run();

	}

}
