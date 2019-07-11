
/* *********************************************************************** *
 * project: org.matsim.*
 * JDEQSimModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JDEQSimModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "JDEQEngine";

	@Override
	protected void configureQSim() {
		addNamedComponent(JDEQSimEngine.class, COMPONENT_NAME);
	}

	@Provides
	@Singleton
	public JDEQSimEngine provideJDEQSimulation(QSim qsim) {
		SteppableScheduler scheduler = new SteppableScheduler(new MessageQueue());
		return new JDEQSimEngine(
				ConfigUtils.addOrGetModule(qsim.getScenario().getConfig(), JDEQSimConfigGroup.NAME,
						JDEQSimConfigGroup.class),
				qsim.getScenario(), qsim.getEventsManager(), qsim.getAgentCounter(), scheduler);
	}
}
