package org.matsim.core.mobsim.qsim;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;

import com.google.inject.Module;

public class PopulationPlugin extends AbstractQSimPlugin {
	public final static String POPULATION_SOURCE_NAME = "PopulationSource";

	public PopulationPlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(PopulationAgentSource.class).asEagerSingleton();
				if (getConfig().transit().isUseTransit()) {
					bind(AgentFactory.class).to(TransitAgentFactory.class).asEagerSingleton();
				} else {
					bind(AgentFactory.class).to(DefaultAgentFactory.class).asEagerSingleton();
				}
			}
		});
	}

	@Override
	public Map<String, Class<? extends AgentSource>> agentSources() {
		return Collections.singletonMap(POPULATION_SOURCE_NAME, PopulationAgentSource.class);
	}
}
