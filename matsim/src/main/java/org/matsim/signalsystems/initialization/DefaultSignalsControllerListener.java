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
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.model.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;


/**
 * @author dgrether
 *
 */
public class DefaultSignalsControllerListener implements SignalsControllerListener, StartupListener, ShutdownListener, IterationStartsListener {

	private SignalSystemsManager signalManager;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		//load data
		SignalsData signalsData = event.getControler().getScenario().getScenarioElement(SignalsData.class);
		//build model
		FromDataBuilder modelBuilder = new FromDataBuilder(signalsData, event.getControler().getEvents());
		this.signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		//init mobility simulation
		QSimSignalEngine signalEngie = new QSimSignalEngine(this.signalManager);
		event.getControler().getQueueSimulationListener().add(signalEngie);
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.signalManager.resetModel(event.getIteration());
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.writeData(event.getControler().getScenario(), event.getControler().getControlerIO().getOutputPath());
	}
	
	public void writeData(Scenario sc, String outputPath){
		SignalsData data = sc.getScenarioElement(SignalsData.class);
		new SignalsScenarioWriter(outputPath).writeSignalsData(data);
	}

}
