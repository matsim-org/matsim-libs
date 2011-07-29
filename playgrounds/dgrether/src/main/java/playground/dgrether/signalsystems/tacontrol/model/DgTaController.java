/* *********************************************************************** *
 * project: org.matsim.*
 * DgTaController
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
package playground.dgrether.signalsystems.tacontrol.model;

import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class DgTaController implements SignalController {

	@Override
	public void updateState(double timeSeconds) {
	}

	@Override
	public void addPlan(SignalPlan plan) {
	}

	@Override
	public void setSignalSystem(SignalSystem system) {
	}

	@Override
	public void reset(Integer iterationNumber) {
	}

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
	}

}
