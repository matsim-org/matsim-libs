/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dynagent.run;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public final class DynAgentSourcePlugin extends AbstractQSimPlugin {
	public final static String DYN_AGENT_SOURCE_NAME = "DynAgentSource";

	private final Class<? extends AgentSource> agentSourceClass;

	public DynAgentSourcePlugin(Config config, Class<? extends AgentSource> agentSourceClass) {
		super(config);
		this.agentSourceClass = agentSourceClass;
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			public void configure() {
				bind(agentSourceClass).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends AgentSource>> agentSources() {
		return Collections.singletonMap(DYN_AGENT_SOURCE_NAME, agentSourceClass);
	}
}
