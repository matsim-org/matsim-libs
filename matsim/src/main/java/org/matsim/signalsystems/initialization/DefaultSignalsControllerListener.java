/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsControlerListener
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
package org.matsim.signalsystems.initialization;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.builder.SignalSystemsModelBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.model.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;


/**
 * @author dgrether
 *
 */
public class DefaultSignalsControllerListener implements StartupListener, ShutdownListener, IterationStartsListener {

	private SignalSystemsModelBuilder modelBuilder;
	private SignalSystemsManager signalManager;
	private SignalsData signalsData;
	private QSimSignalEngine signalEngie;
	
	
	
	@Override
	public void notifyStartup(StartupEvent event) {
		this.loadData(event.getControler().getConfig().signalSystems(), event.getControler().getScenario());
		this.modelBuilder = new FromDataBuilder(signalsData, event.getControler().getEvents());
		
		this.createModel();
		event.getControler().getQueueSimulationListener().add(this.signalEngie);
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.signalManager.resetModel(event.getIteration());
	}

	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		new SignalsScenarioWriter(event.getControler().getControlerIO().getOutputPath()).writeSignalsData(this.signalsData);
	}

	private SignalsData loadData(SignalSystemsConfigGroup config, Scenario scenario) {
		SignalsScenarioLoader loader = new SignalsScenarioLoader(config);
		this.signalsData = loader.loadSignalsData();
		scenario.addScenarioElement(this.signalsData);
		return signalsData;
	}

	private void createModel() {
		this.signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		this.signalEngie = new QSimSignalEngine(this.signalManager);
	}




	

}
