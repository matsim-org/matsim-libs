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
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.DatabasedSignalPlan;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.data.Lanes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.advancedPlanbased.AdvancedPlanBasedSignalSystemController;
import signals.downstreamSensor.DownstreamPlanbasedSignalController;
import signals.downstreamSensor.DownstreamSensor;
import signals.gershenson.DgRoederGershensonSignalController;
import signals.laemmer.model.LaemmerConfig;
import signals.laemmer.model.LaemmerSignalController;
import signals.sensor.LinkSensorManager;
import signals.sylvia.controler.DgSylviaConfig;
import signals.sylvia.data.DgSylviaPreprocessData;
import signals.sylvia.model.DgSylviaSignalPlan;
import signals.sylvia.model.SylviaSignalController;

/**
 * combined signal model factory that works for all provided signal controller, so far: planbased, sylvia, downstream, laemmer, gershenson
 * 
 * @author tthunig
 *
 */
public class CombinedSignalModelFactory implements SignalModelFactory {

	private static final Logger log = Logger.getLogger(CombinedSignalModelFactory.class);

	private SignalModelFactory delegate = new DefaultSignalModelFactory();

	private Map<String, Provider<SignalController>> signalControlProvider = new HashMap<>();
	
	private Scenario scenario;

	@Inject
	public CombinedSignalModelFactory(Scenario scenario, LaemmerConfig laemmerConfig, DgSylviaConfig sylviaConfig, 
			LinkSensorManager sensorManager, DownstreamSensor downstreamSensor, TtTotalDelay delayCalculator) {
		this.scenario = scenario;
//		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		Network network = scenario.getNetwork();
		Lanes lanes = scenario.getLanes();
		// prepare signal controller provider
		signalControlProvider.put(SylviaSignalController.IDENTIFIER, new SylviaSignalController.SignalControlProvider(sylviaConfig, sensorManager, downstreamSensor));
		signalControlProvider.put(DownstreamPlanbasedSignalController.IDENTIFIER, new DownstreamPlanbasedSignalController.SignalControlProvider(downstreamSensor));
		signalControlProvider.put(LaemmerSignalController.IDENTIFIER, new LaemmerSignalController.SignalControlProvider(laemmerConfig, sensorManager, network, lanes, delayCalculator));
		signalControlProvider.put(DgRoederGershensonSignalController.IDENTIFIER, new DgRoederGershensonSignalController.SignalControlProvider(sensorManager, scenario));
		signalControlProvider.put(AdvancedPlanBasedSignalSystemController.IDENTIFIER, new AdvancedPlanBasedSignalSystemController.SignalControlProvider(sensorManager, delayCalculator));
	}

	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return this.delegate.createSignalSystem(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier, SignalSystem signalSystem) {
		if (signalControlProvider.containsKey(controllerIdentifier)) {
			if (controllerIdentifier.equals(LaemmerSignalController.IDENTIFIER) && scenario.getConfig().qsim().getFlowCapFactor() != 1.0){
				throw new RuntimeException("Laemmer signal control does not support flow capacity factors different from 1.0");
			}
			log.info("Creating " + controllerIdentifier);
			SignalController signalControl = signalControlProvider.get(controllerIdentifier).get();
			signalControl.setSignalSystem(signalSystem);
			return signalControl;
		}
		return this.delegate.createSignalSystemController(controllerIdentifier, signalSystem);
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
		DatabasedSignalPlan plan = (DatabasedSignalPlan) this.delegate.createSignalPlan(planData);
		if (planData.getId().toString().startsWith(DgSylviaPreprocessData.SYLVIA_PREFIX)) {
			return new DgSylviaSignalPlan(plan);
		}
		return plan;
	}

}
