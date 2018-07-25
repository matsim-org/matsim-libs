package org.matsim.core.mobsim.qsim.jdeqsimengine;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JDEQSimPlugin extends AbstractQSimPlugin {
	public static String JDEQ_ENGINE = "JDEQEngine";

	public JDEQSimPlugin(Config config) {
		super(config);
	}

	public Collection<? extends Module> modules() {
		return Collections.singleton(new AbstractModule() {
			@Override
			protected void configure() {

			}

			@Provides
			@Singleton
			public JDEQSimEngine provideJDEQSimulation(QSim qsim) {
				SteppableScheduler scheduler = new SteppableScheduler(new MessageQueue());
				return new JDEQSimEngine(
						ConfigUtils.addOrGetModule(qsim.getScenario().getConfig(), JDEQSimConfigGroup.NAME,
								JDEQSimConfigGroup.class),
						qsim.getScenario(), qsim.getEventsManager(), qsim.getAgentCounter(), scheduler);
			}
		});
	}

	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(JDEQ_ENGINE, JDEQSimEngine.class);
	}

	public Map<String, Class<? extends ActivityHandler>> activityHandlers() {
		return Collections.singletonMap(JDEQ_ENGINE, JDEQSimEngine.class);
	}
}
