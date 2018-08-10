/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultSignalModelFactory
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
package org.matsim.contrib.signals.builder;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.DatabasedSignalPlan;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemImpl;

import com.google.inject.Provider;


/**
 * 
 * @author tthunig
 */
public final class SignalModelFactoryImpl implements SignalModelFactory {
	
	private static final Logger log = Logger.getLogger(SignalModelFactoryImpl.class);
	
	private final Map<String, Provider<SignalController>> signalControlProvider;
	
	public SignalModelFactoryImpl(Map<String, Provider<SignalController>> signalControlProvider) {
		this.signalControlProvider = signalControlProvider;
	}
	
	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return new SignalSystemImpl(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier, SignalSystem signalSystem) {
		if (signalControlProvider.containsKey(controllerIdentifier)) {
			log.info("Creating " + controllerIdentifier);
			SignalController signalControl = signalControlProvider.get(controllerIdentifier).get();
			signalControl.setSignalSystem(signalSystem);
			return signalControl;
		}
		throw new RuntimeException("Signal controller " + controllerIdentifier + " not specified. "
				+ "Add a respective provider to the SignalsModule by calling the method addSignalControlProvider.");
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
//		DatabasedSignalPlan plan = new DatabasedSignalPlan(planData);
//		if (planData.getId().toString().startsWith(SylviaPreprocessData.SYLVIA_PREFIX)) {
//			return new SylviaSignalPlan(plan);
//		}
//		return plan;
		return new DatabasedSignalPlan(planData);
	}
}
