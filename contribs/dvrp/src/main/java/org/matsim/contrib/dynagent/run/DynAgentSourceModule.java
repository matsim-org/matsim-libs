package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class DynAgentSourceModule extends AbstractQSimModule {
	public final static String DYN_AGENT_SOURCE_NAME = "DynAgentSource";

	private final Class<? extends AgentSource> agentSourceClass;

	public DynAgentSourceModule(Class<? extends AgentSource> agentSourceClass) {
		this.agentSourceClass = agentSourceClass;
	}

	@Override
	protected void configureQSim() {
		bind(agentSourceClass).asEagerSingleton();
		addAgentSource(DYN_AGENT_SOURCE_NAME).to(agentSourceClass);
	}
}
