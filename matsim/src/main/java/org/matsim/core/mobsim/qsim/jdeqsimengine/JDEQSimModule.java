package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JDEQSimModule extends AbstractQSimModule {
	public static String JDEQ_ENGINE_NAME = "JDEQEngine";

	@Override
	protected void configureQSim() {
		addMobsimEngineBinding(JDEQ_ENGINE_NAME).to(JDEQSimEngine.class);
		addActivityHandlerBinding(JDEQ_ENGINE_NAME).to(JDEQSimEngine.class);
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
}
