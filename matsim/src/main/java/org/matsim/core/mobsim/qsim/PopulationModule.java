
/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationModule.java
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

 package org.matsim.core.mobsim.qsim;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.pt.config.TransitConfigGroup;

public class PopulationModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "PopulationAgentSource";

	@Override
	protected void configureQSim() {
		bind(PopulationAgentSource.class).asEagerSingleton();
		addQSimComponentBinding( COMPONENT_NAME ).to( PopulationAgentSource.class );
	}

	@Provides
	@Singleton
	AgentFactory provideAgentFactory(TransitConfigGroup config, Netsim simulation, TimeInterpretation timeInterpretation) {
		if (config.isUseTransit()) {
			return new TransitAgentFactory(simulation, timeInterpretation);
		} else {
			return new DefaultAgentFactory(simulation, timeInterpretation);
		}
	}
	
	@Provides
	@Singleton
	QVehicleFactory provideQVehicleFactory( ) {
		return QVehicleImpl::new;
	}
}
