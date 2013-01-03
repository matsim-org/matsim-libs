/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DControllerListener
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v3.controller;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.controler.SignalsControllerListener;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.gregor.sim2d_v3.simulation.HybridQ2DMobsimFactory;


/**
 * @author dgrether
 *
 */
public class Sim2DSignalsControllerListener implements SignalsControllerListener, StartupListener, ShutdownListener, IterationStartsListener {

	private SignalSystemsManager signalManager;
	private final HybridQ2DMobsimFactory hQ2DFac;
	
	public Sim2DSignalsControllerListener(HybridQ2DMobsimFactory hQ2DFac) {
		this.hQ2DFac = hQ2DFac;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		//build model
		FromDataBuilder modelBuilder = new FromDataBuilder(event.getControler().getScenario(), event.getControler().getEvents());
		this.signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		//init mobility simulation
		Sim2DSignalEngine signalEngie = new Sim2DSignalEngine(this.signalManager,this.hQ2DFac);
		event.getControler().getMobsimListeners().add(signalEngie);
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.signalManager.resetModel(event.getIteration());
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.writeData(event.getControler().getScenario(), event.getControler().getControlerIO());
	}
	
	public void writeData(Scenario sc, OutputDirectoryHierarchy controlerIO){
		SignalsData data = sc.getScenarioElement(SignalsData.class);
		new SignalsScenarioWriter(controlerIO).writeSignalsData(data);
	}
	
}