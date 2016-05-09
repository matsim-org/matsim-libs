/* *********************************************************************** *
 * project: org.matsim.*
 * DgSylviaSignalControlerListener
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
package playground.dgrether.signalsystems.sylvia.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controler.SignalsControllerListener;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.inject.Inject;

import playground.dgrether.signalsystems.DgSensorManager;


/**
 * @author dgrether, tthunig
 */
public class DgSylviaSignalControlerListener implements SignalsControllerListener, IterationStartsListener,
		ShutdownListener {

	@Inject private SignalSystemsManager signalManager;
	@Inject private DgSensorManager sensorManager;
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.signalManager.resetModel(event.getIteration());
		this.sensorManager.reset(event.getIteration());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.writeData(event.getServices().getScenario(), event.getServices().getControlerIO());
	}
	
	public void writeData(Scenario sc, OutputDirectoryHierarchy controlerIO){
		new SignalsScenarioWriter(controlerIO).writeSignalsData(sc);
	}	
	
}
