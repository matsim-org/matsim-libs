/* *********************************************************************** *
 * project: org.matsim.*
 * DgSylviaSignalModelFactory
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
package playground.dgrether.signalsystems.sylvia.model;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.signals.model.DatabasedSignalPlan;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.data.DgSylviaPreprocessData;


/**
 * @author dgrether
 *
 */
public final class DgSylviaSignalModelFactory implements SignalModelFactory {

	
	private static final Logger log = Logger.getLogger(DgSylviaSignalModelFactory.class);
	
	private SignalModelFactory delegate;

	private DgSensorManager sensorManager;

	private DgSylviaConfig sylviaConfig;
	
	public DgSylviaSignalModelFactory(SignalModelFactory delegate, DgSensorManager sensorManager, DgSylviaConfig sylviaConfig) {
		this.delegate = delegate;
		this.sensorManager = sensorManager;
		this.sylviaConfig = sylviaConfig;
	}

	@Override
	public SignalSystemsManager createSignalSystemsManager() {
		return this.delegate.createSignalSystemsManager();
	}

	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return this.delegate.createSignalSystem(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier) {
		if (DgSylviaController.CONTROLLER_IDENTIFIER.equals(controllerIdentifier)){
			log.info("Creating " + DgSylviaController.CONTROLLER_IDENTIFIER);
			return new DgSylviaController(this.sylviaConfig, this.sensorManager);
		}
		return this.delegate.createSignalSystemController(controllerIdentifier);
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
		DatabasedSignalPlan plan = new DatabasedSignalPlan(planData);
		if (planData.getId().toString().startsWith(DgSylviaPreprocessData.SYLVIA_PREFIX)){
			return new DgSylviaSignalPlan(plan);
		}
		return plan;
	}

}
