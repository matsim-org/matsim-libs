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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.DatabasedSignalPlan;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.SignalSystemImpl;
import org.matsim.contrib.signals.model.SignalSystemsManagerImpl;
import org.matsim.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signals.model.SignalController;
import org.matsim.signals.model.SignalPlan;
import org.matsim.signals.model.SignalSystem;
import org.matsim.signals.model.SignalSystemsManager;


/**
 * 
 * @author dgrether
 */
public final class DefaultSignalModelFactory implements SignalModelFactory {
	
	private static final Logger log = Logger.getLogger(DefaultSignalModelFactory.class);
	
	@Override
	public SignalSystemsManager createSignalSystemsManager() {
		return new SignalSystemsManagerImpl();
	}

	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return new SignalSystemImpl(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier) {
		if (DefaultPlanbasedSignalSystemController.IDENTIFIER.equals(controllerIdentifier)){
			log.info("Created SignalController: " + DefaultPlanbasedSignalSystemController.IDENTIFIER);
			return new DefaultPlanbasedSignalSystemController();
		}
		//TODO improve error message and consider creation by class name
		throw new IllegalArgumentException("Controller " + controllerIdentifier + " not known.");
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
		return new DatabasedSignalPlan(planData);
	}
	
	
}
