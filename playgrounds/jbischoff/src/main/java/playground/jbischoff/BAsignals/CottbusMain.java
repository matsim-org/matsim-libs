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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.model.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.signalsystems.model.SignalSystemsManagerImpl;


/**
 * @author dgrether
 *
 */
public class CottbusMain implements StartupListener, IterationStartsListener{

	private String config = JbBaPaths.BASIMW+"scenario-lsa/cottbusConfig.xml";
	private SignalSystemsManager manager;
	
	public void runCottbus(){
		Controler controler = new Controler(config);
		controler.getConfig().scenario().setUseSignalSystems(false);
		controler.addControlerListener(this);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.manager.resetModel(event.getIteration());
	}

	@Override
	public void notifyStartup(StartupEvent e) {
		Controler c = e.getControler();
		Scenario scenario = c.getScenario();
		SignalSystemsConfigGroup signalsConfig = scenario.getConfig().signalSystems();
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(signalsConfig);
		SignalsData signalsData = signalsLoader.loadSignalsData();
		FromDataBuilder builder = new FromDataBuilder(signalsData, c.getEvents());
		JbSignalBuilder jbBuilder = new JbSignalBuilder(signalsData, builder);
		this.manager = jbBuilder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		c.getQueueSimulationListener().add(engine);
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CottbusMain().runCottbus();
	}


}
