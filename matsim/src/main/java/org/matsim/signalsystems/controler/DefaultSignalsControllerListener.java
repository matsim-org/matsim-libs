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
package org.matsim.signalsystems.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;


/**
 * SignalControllerListener implementation for the MATSim default implementation for traffic light control, 
 * i.e. a fixed-time traffic signal control that can be specified completely by xml input data.
 * @author dgrether
 *
 */
public final class DefaultSignalsControllerListener implements SignalsControllerListener, ShutdownListener, IterationStartsListener {

	private QSimSignalEngine signalEngine;

	@Override
	public final void notifyIterationStarts(IterationStartsEvent event) {
		event.getControler().getMobsimListeners().remove(this.signalEngine);
		//build model
		FromDataBuilder modelBuilder = new FromDataBuilder(event.getControler().getScenario(), event.getControler().getEvents());
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		//init mobility simulation
		this.signalEngine = new QSimSignalEngine(signalManager);
		event.getControler().getMobsimListeners().add(signalEngine);
		signalManager.resetModel(event.getIteration());
	}
	
	@Override
	public final void notifyShutdown(ShutdownEvent event) {
		writeData(event.getControler().getScenario(), event.getControler().getControlerIO());
	}
	
	private static void writeData(Scenario sc, OutputDirectoryHierarchy controlerIO){
		SignalsData data = (SignalsData) sc.getScenarioElement(SignalsData.ELEMENT_NAME);
		new SignalsScenarioWriter(controlerIO).writeSignalsData(data);
	}

}
