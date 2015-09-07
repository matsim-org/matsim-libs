package org.matsim.core.mobsim.qsim.messagequeueengine;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableScheduler;

public class MessageQueueEngineModule extends AbstractModule {

	@Override
	public void install() {
		bind(MessageQueue.class).asEagerSingleton(); // public
		bind(SteppableScheduler.class).asEagerSingleton();
		bind(MessageQueueEngine.class).asEagerSingleton();
	}

}
