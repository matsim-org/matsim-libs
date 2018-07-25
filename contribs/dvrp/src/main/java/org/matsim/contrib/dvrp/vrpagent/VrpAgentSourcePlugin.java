package org.matsim.contrib.dvrp.vrpagent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class VrpAgentSourcePlugin extends AbstractQSimPlugin {
	public final static String VRP_AGENT_SOURCE_NAME = "VrpAgentSource";
	
	public VrpAgentSourcePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			public void configure() {
				bind(VrpAgentSource.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends AgentSource>> agentSources() {
		return Collections.singletonMap(VRP_AGENT_SOURCE_NAME, VrpAgentSource.class);
	}
}
