/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusMain
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.model.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;


/**
 * @author dgrether
 *
 */
public class CottbusMain {

	private String config = "";
	
	public void runCottbus(){
		ScenarioLoader loader = new ScenarioLoaderImpl(config);
		Scenario scenario = loader.loadScenario();

		EventsManager events = new EventsManagerImpl();
		
		SignalEngine signalEngine = this.initSignalEngine(scenario.getConfig().signalSystems(), events);
		
		QSim qsim = new QSim(scenario, events);
		qsim.addQueueSimulationListeners(signalEngine);
		qsim.run();
		
	}
	
	private SignalEngine initSignalEngine(SignalSystemsConfigGroup signalsConfig, EventsManager events) {
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(signalsConfig);
		SignalsData signalsData = signalsLoader.loadSignalsData();

		FromDataBuilder builder = new FromDataBuilder(signalsData, events);
		JbSignalBuilder jbBuilder = new JbSignalBuilder(signalsData, builder);
		SignalSystemsManager manager = jbBuilder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		return engine;
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CottbusMain().runCottbus();
	}

}
