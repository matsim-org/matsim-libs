/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsPerception.java
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

package playground.gregor.sim2d_v3.simulation.floor.forces.deliberative;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.model.SignalGroupState;

import playground.gregor.sim2d_v3.controller.PedestrianSignal;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.forces.ForceModule;

public class SignalsPerception implements ForceModule {

	
	private final Map<Id, PedestrianSignal> signals;

	public SignalsPerception(Map<Id, PedestrianSignal> signals) {
		this.signals = signals;
	}
	
	@Override
	public void run(Agent2D agent, double time) {
		PedestrianSignal sig = this.signals.get(agent.getDelegate().getCurrentLinkId());
		if (sig != null) {
			if (!sig.hasGreenForToLink(agent.getDelegate().chooseNextLinkId())) {
				agent.informAboutSignalState(SignalGroupState.RED, time);
			} else {
				agent.informAboutSignalState(SignalGroupState.GREEN, time);
			}	
		}
		
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

}
