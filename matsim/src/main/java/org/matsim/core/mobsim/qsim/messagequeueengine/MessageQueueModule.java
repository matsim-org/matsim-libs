package org.matsim.core.mobsim.qsim.messagequeueengine;

import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableScheduler;

public class MessageQueueModule extends AbstractQSimModule {
	static public final String COMPONENT_NAME = "MessageQueueEngine";

	@Override
	protected void configureQSim() {
		bind(MessageQueue.class).asEagerSingleton();
		bind(SteppableScheduler.class).asEagerSingleton();
		bind(MessageQueueEngine.class).asEagerSingleton();
		
		addNamedComponent(MessageQueueEngine.class, COMPONENT_NAME);
	}
}
