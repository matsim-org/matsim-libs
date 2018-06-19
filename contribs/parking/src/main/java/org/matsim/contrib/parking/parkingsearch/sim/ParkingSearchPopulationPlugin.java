/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.parking.parkingsearch.sim;

import java.util.Collection;
import java.util.Collections;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;

import com.google.inject.Module;

/**
 * @author  jbischoff
 *
 */
public class ParkingSearchPopulationPlugin extends AbstractQSimPlugin {
	public ParkingSearchPopulationPlugin(Config config) { super(config); }
	@Override 
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				if (getConfig().transit().isUseTransit()) {
					throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
				} 
				bind(AgentFactory.class).to(ParkingAgentFactory.class).asEagerSingleton(); // (**)
				bind(ParkingPopulationAgentSource.class).asEagerSingleton();
			}
		});
	}
	@Override 
	public Collection<Class<? extends AgentSource>> agentSources() {
		return Collections.singletonList(ParkingPopulationAgentSource.class);
	}
}