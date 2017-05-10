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
package signals.sensor;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controler.SignalControlerListener;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.inject.Inject;


/**
 * also works without sensor-based signals, i.e. plan-based signals and also without signals at all.
 * 
 * @author dgrether, tthunig
 */
public class SensorBasedSignalControlerListener implements SignalControlerListener, IterationStartsListener,
		ShutdownListener {

	@Inject(optional = true) SignalSystemsManager signalManager = null;
	@Inject(optional = true) LinkSensorManager sensorManager = null;
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (this.signalManager != null) 
			this.signalManager.resetModel(event.getIteration());
		if (this.sensorManager != null)
			this.sensorManager.reset(event.getIteration());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.writeData(event.getServices().getScenario(), event.getServices().getControlerIO());
	}
	
	private void writeData(Scenario sc, OutputDirectoryHierarchy controlerIO){
		new SignalsScenarioWriter(controlerIO).writeSignalsData(sc);
	}	
	
}
