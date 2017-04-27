/* *********************************************************************** *
 * project: org.matsim.*
 * DgGershensonRoederSignalModelFactory
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
package signals.gershenson;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;

import com.google.inject.Inject;

import playground.dgrether.signalsystems.sensor.LinkSensorManager;
import signals.gershenson.DgRoederGershensonSignalController.SignalControlProvider;


/**
 * @author dgrether
 *
 */
public final class DgRoederGershensonSignalModelFactory implements SignalModelFactory {
	
	private static final Logger log = Logger.getLogger(DgRoederGershensonSignalModelFactory.class);
	
	private SignalModelFactory delegate = new DefaultSignalModelFactory();
	
	private SignalControlProvider provider;
	
	@Inject 
	public DgRoederGershensonSignalModelFactory(Scenario scenario, LinkSensorManager sensorManager) {
		provider = new DgRoederGershensonSignalController.SignalControlProvider(sensorManager, scenario);
	}
	
	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return this.delegate.createSignalSystem(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier, SignalSystem signalSystem) {
		if (DgRoederGershensonSignalController.IDENTIFIER.equals(controllerIdentifier)){
			log.info("Created controller: " + DgRoederGershensonSignalController.IDENTIFIER);
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
