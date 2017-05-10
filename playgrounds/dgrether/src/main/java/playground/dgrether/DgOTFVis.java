package playground.dgrether;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.apache.log4j.Logger;
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
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.util.Types;

import playground.dgrether.utils.DgOTFVisUtils;

/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVis
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

/**
 * @author dgrether
 *
 */
public class DgOTFVis {
	
	
	private static final Logger log = Logger.getLogger(DgOTFVis.class);
	
	
	public void playScenario(Scenario scenario) {

		if (ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()){
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		}
		
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
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		OTFClientLive.run(scenario.getConfig(), server);
		qSim.run();
	}
	
	public void playAndRouteConfig(String config){
		Config cc = ConfigUtils.loadConfig(config);
		ConfigUtils.addOrGetModule(cc, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setMapOverlayMode(true);
		MutableScenario sc = (MutableScenario) ScenarioUtils.loadScenario(cc);
		DgOTFVisUtils.preparePopulation4Simulation(sc);
		this.playScenario(sc);
	}
	
	public static void  printClasspath(){
		System.out.println("Classpath: ");
	//Get the System Classloader
    ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
    //Get the URLs
    URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
    for(int i=0; i< urls.length; i++)
    {
        System.out.println("  " + urls[i].getFile());
    }
	}

	
	public static void main(String[] args) {
		new DgOTFVis().playAndRouteConfig(args[0]);
	}
}
