package org.matsim.pt.withinday;

import java.util.Collection;
import java.util.Collections;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;

import com.google.inject.Module;

public class WithinDayTransitPopulationPlugin extends AbstractQSimPlugin {

	public WithinDayTransitPopulationPlugin(Config config) {
		super(config);
	}
	
	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(PopulationAgentSource.class).asEagerSingleton();
				if (getConfig().transit().isUseTransit()) {
					bind(AgentFactory.class).to(ScriptedTransitAgentFactory.class).asEagerSingleton();
				} else {
					bind(AgentFactory.class).to(DefaultAgentFactory.class).asEagerSingleton();
				}
			}
		});
	}

	@Override
	public Collection<Class<? extends AgentSource>> agentSources() {
		return Collections.singletonList(PopulationAgentSource.class);
	}

}
