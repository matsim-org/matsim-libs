package org.matsim.contrib.carsharing.relocation.qsim;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;

import com.google.inject.Module;

public class RelocationQSimPlugin extends AbstractQSimPlugin {

	public RelocationQSimPlugin(Config config) {
		super(config);
	}

	public Collection<? extends Module> modules() {

		return Collections.singletonList(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				install(new RelocationModule());

			}
		});
	}

	public Collection<Class<? extends AgentSource>> agentSources() {
		return Arrays.asList(RelocationAgentSource.class);
	}

}
