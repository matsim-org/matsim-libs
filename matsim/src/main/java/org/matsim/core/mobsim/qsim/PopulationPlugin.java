package org.matsim.core.mobsim.qsim;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;

import java.util.ArrayList;
import java.util.Collection;

public class PopulationPlugin extends AbstractQSimPlugin {

	@Override
	public Collection<? extends AbstractModule> modules() {
		Collection<AbstractModule> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void install() {
				bind(PopulationAgentSource.class).asEagerSingleton();
				if (getConfig().transit().isUseTransit()) {
					bind(AgentFactory.class).to(TransitAgentFactory.class).asEagerSingleton();
				} else {
					bind(AgentFactory.class).to(DefaultAgentFactory.class).asEagerSingleton();
				}
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends AgentSource>> agentSources() {
		Collection<Class<? extends AgentSource>> result = new ArrayList<>();
		result.add(PopulationAgentSource.class);
		return result;
	}
}
