/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010OtfVis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.figure9scenario;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.util.Types;

import playground.dgrether.DgOTFVis;



public class DgFigure9OtfVis {
	
	private void runFromConfig() {
//		String conf = DgKoehlerStrehler2010Runner.signalsConfigSol800;
//		String conf = "/media/data/work/repos/shared-svn/studies/dgrether/koehlerStrehler2010/scenario2/config_signals_coordinated.xml";
		String conf = "../../../shared-svn/studies/dgrether/koehlerStrehler2010/scenario5/config_signals_coordinated.xml";
		
		Config config = ConfigUtils.loadConfig(conf);
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setAgentSize(40.0f);
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				// defaults
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
				// signal specific module
				install(new SignalsModule());
			}
		});
	
		EventsManager events = injector.getInstance(EventsManager.class);
		events.initProcessing();
		
		QSim qSim = (QSim) injector.getInstance(Mobsim.class);
		Collection<Provider<MobsimListener>> mobsimListeners = (Collection<Provider<MobsimListener>>) injector.getInstance(Key.get(Types.collectionOf(Types.providerOf(MobsimListener.class))));
		for (Provider<MobsimListener> provider : mobsimListeners) {
			qSim.addQueueSimulationListeners(provider.get());
		}
		
		DgOTFVis.printClasspath();
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		OTFClientLive.run(scenario.getConfig(), server);
		
		qSim.run();
	}
	
	
	public static void main(String[] args) {
		new DgFigure9OtfVis().runFromConfig();
	}
}
