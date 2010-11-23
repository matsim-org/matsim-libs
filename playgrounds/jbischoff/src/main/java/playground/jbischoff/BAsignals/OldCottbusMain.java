/* *********************************************************************** *
 * project: org.matsim.*
 * OldCottbusMain.java
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

package playground.jbischoff.BAsignals;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;

public class OldCottbusMain {
private String config = JbBaPaths.BASIMW+"/scenario-lsa/cottbusConfig.xml";
	
	public void runCottbus(){
		ScenarioLoader loader = new ScenarioLoaderImpl(config);
		ScenarioImpl scenario = (ScenarioImpl) loader.loadScenario();
		
//		ControlerIO controlerIo = new ControlerIO(scenario.getConfig().controler().getOutputDirectory());
		System.out.println(scenario.getNetwork().getLinks().get(new IdImpl(6495)).getLength());
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.run();
		
//		EventsManager events = new EventsManagerImpl();
//		SignalEngine signalEngine = this.initSignalEngine(scenario.getConfig().signalSystems(), events);
//		QSim qsim = new QSim(scenario, events);
//		qsim.setControlerIO();
//		qsim.addQueueSimulationListeners(signalEngine);
//		qsim.run();
		
		
		
	}
	
	private SignalEngine initSignalEngine(SignalSystemsConfigGroup signalsConfig, EventsManager events) {
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(signalsConfig);
		SignalsData signalsData = signalsLoader.loadSignalsData();

		FromDataBuilder builder = new FromDataBuilder(signalsData, events);
		JbSignalBuilder jbBuilder = new JbSignalBuilder(signalsData, builder, null);
		SignalSystemsManager manager = jbBuilder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		return engine;
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new OldCottbusMain().runCottbus();
	}

}

