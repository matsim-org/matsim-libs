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
package signals.laemmer.model;

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

import org.matsim.lanes.data.Lanes;
import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.laemmer.model.LaemmerSignalController.SignalControlProvider;
import signals.sensor.LinkSensorManager;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public final class LaemmerSignalModelFactory implements SignalModelFactory {

	private static final Logger log = Logger.getLogger(LaemmerSignalModelFactory.class);
	
	private DefaultSignalModelFactory delegate = new DefaultSignalModelFactory();

	private SignalControlProvider provider;
	
	@Inject
	public LaemmerSignalModelFactory(LaemmerConfig config, LinkSensorManager sensorManager, Scenario scenario, TtTotalDelay delayCalculator) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		Network network = scenario.getNetwork();
		Lanes lanes = scenario.getLanes();
		this.provider = new LaemmerSignalController.SignalControlProvider(config, sensorManager,  network, lanes, delayCalculator);
	}

	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return this.delegate.createSignalSystem(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier, SignalSystem signalSystem) {
		if (LaemmerSignalController.IDENTIFIER.equals(controllerIdentifier)){
			log.info("Creating " + LaemmerSignalController.IDENTIFIER);
			SignalController signalControl = this.provider.get();
			signalControl.setSignalSystem(signalSystem);
			return signalControl;
		}
		return this.delegate.createSignalSystemController(controllerIdentifier, signalSystem);
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
		return this.delegate.createSignalPlan(planData);
	}

}
