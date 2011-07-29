/* *********************************************************************** *
 * project: org.matsim.*
 * DgTaSignalModelFactory
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

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.builder.DefaultSignalModelFactory;
import org.matsim.signalsystems.builder.SignalModelFactory;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.dgrether.signalsystems.DgSensorManager;


/**
 * @author dgrether
 *
 */
public class DgTaSignalModelFactory implements SignalModelFactory {

	public DgTaSignalModelFactory(DefaultSignalModelFactory defaultSignalModelFactory,
			DgSensorManager sensorManager) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public SignalSystemsManager createSignalSystemsManager() {
		return null;
	}

	@Override
	public SignalSystem createSignalSystem(Id id) {
		return null;
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier) {
		return null;
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
		return null;
	}

}
