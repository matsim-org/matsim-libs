package org.matsim.core.mobsim.qsim.messagequeueengine;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MessageQueuePlugin extends AbstractQSimPlugin{

	@Override
	public Collection<? extends AbstractModule> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			public void install() {
				bind(MessageQueue.class).asEagerSingleton();
				bind(SteppableScheduler.class).asEagerSingleton();
				bind(MessageQueueEngine.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Collection<Class<? extends MobsimListener>> listeners() {
		Collection<Class<? extends MobsimListener>> result = new ArrayList<>();
		result.add(MessageQueueEngine.class);
		return result;
	}
}
