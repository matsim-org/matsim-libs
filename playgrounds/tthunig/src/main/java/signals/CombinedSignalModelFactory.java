/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package signals;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;

import com.google.inject.Inject;

import playground.dgrether.signalsystems.LinkSensorManager;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.model.SylviaSignalController;
import signals.downstreamSensor.DownstreamSignalController;

/**
 * @author tthunig
 *
 */
public class CombinedSignalModelFactory implements SignalModelFactory {

	private static final Logger log = Logger.getLogger(CombinedSignalModelFactory.class);

	private SignalModelFactory delegate = new DefaultSignalModelFactory();

	private Map<String, Object> signalControllerBuilder = new HashMap<>();

	@Inject(optional = true) LinkSensorManager sensorManager = null;
	@Inject(optional = true) DgSylviaConfig sylviaConfig = null;
	@Inject	Scenario scenario;

	public CombinedSignalModelFactory() {
		// prepare signal controller builder
		if (sensorManager != null) {
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			if (sylviaConfig != null) {
				signalControllerBuilder.put(SylviaSignalController.IDENTIFIER, new SylviaSignalController.Builder(sylviaConfig, sensorManager, signalsData));
			}
			Network network = scenario.getNetwork();
			signalControllerBuilder.put(DownstreamSignalController.IDENTIFIER, new DownstreamSignalController.Builder(sensorManager, signalsData, network));
		}
	}

	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return this.delegate.createSignalSystem(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier, SignalSystem signalSystem) {
		if (signalControllerBuilder.containsKey(controllerIdentifier)) {
			log.info("Creating " + controllerIdentifier);
			// TODO
//			return signalControllerBuilder.get(controllerIdentifier).build(signalSystem);
		}
		return this.delegate.createSignalSystemController(controllerIdentifier, signalSystem);
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
		return this.delegate.createSignalPlan(planData);
	}

}
