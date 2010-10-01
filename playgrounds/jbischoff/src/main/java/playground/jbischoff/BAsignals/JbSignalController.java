/* *********************************************************************** *
 * project: org.matsim.*
 * JbSignalController
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

import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class JbSignalController implements SignalController {

	private SignalSystem system;

	@Override
	public void addPlan(SignalPlan plan) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSignalSystem(SignalSystem system) {
		this.system = system;
	}

	@Override
	public void updateState(double timeSeconds) {
		SignalsData signalData = this.system.getSignalSystemsManager().getSignalsData();
		
		
	}

}
