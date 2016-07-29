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
package org.matsim.contrib.signals.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;


/**
 * SignalControllerListener implementation for the MATSim default implementation for traffic light control, 
 * i.e. a fixed-time traffic signal control that can be specified completely by xml input data.
 * @author dgrether
 *
 */
final class DefaultSignalsControllerListener implements SignalsControllerListener, ShutdownListener {

	@Override
	public final void notifyShutdown(ShutdownEvent event) {
		writeData(event.getServices().getScenario(), event.getServices().getControlerIO());
	}
	
	private static void writeData(Scenario sc, OutputDirectoryHierarchy controlerIO){
		new SignalsScenarioWriter(controlerIO).writeSignalsData(sc);
	}

}
