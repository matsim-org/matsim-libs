/* *********************************************************************** *
 * project: org.matsim.*
 * SylviaOTFVisMain
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package signals.sylvia.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
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
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.dgrether.utils.DgOTFVisUtils;
import signals.CombinedSignalsModule;

/**
 * @author dgrether
 * @author tthunig
 *
 */
public class SylviaOTFVisMain {

	public void playConfig(String configFileName, boolean prepare4Sim) {
		Config config = ConfigUtils.loadConfig(configFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		
		if (prepare4Sim){ 
			DgOTFVisUtils.preparePopulation4Simulation(scenario);
		}
		
		this.playScenario(scenario);
	}

	
	public void playScenario(Scenario scenario){		
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				// defaults
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
				
				// signal specific module
				install(new CombinedSignalsModule());
			}
		});

		EventsManager events = injector.getInstance(EventsManager.class);
		events.initProcessing();
		
		QSim otfVisQSim = (QSim) injector.getInstance(Mobsim.class);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		
		otfVisQSim.run();
	}
	
	
	public static void main(String[] args) {
		if (args.length < 1){
			System.out.println("Expecting config as first parameter");
			System.out.println("Optionally use --doPrepare4Sim to call PersonPrepareForSim before playing the config.");
			System.exit(0);
		}
		String configFileName = args[0];
		SylviaOTFVisMain sylviaMain = new SylviaOTFVisMain();
		if (args.length < 2){
			sylviaMain.playConfig(configFileName, false);
		}
		else if (args.length == 2){
			if (args[1].equalsIgnoreCase("--doPrepare4Sim")){
				sylviaMain.playConfig(configFileName, true);
			}
		}
	}


}
