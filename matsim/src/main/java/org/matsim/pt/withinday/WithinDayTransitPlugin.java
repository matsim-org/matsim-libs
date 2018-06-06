package org.matsim.pt.withinday;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.Module;

public class WithinDayTransitPlugin extends AbstractQSimPlugin {

	public WithinDayTransitPlugin(Config config) {
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

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		List<Class<? extends MobsimEngine>> result = new ArrayList<>(super.engines());
		result.add(WithinDayTransitEngine.class);
		return result;
	}
	
	

}
