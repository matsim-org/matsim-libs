package org.matsim.core.mobsim.qsim.messagequeueengine;

import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MessageQueuePlugin extends AbstractQSimPlugin{

	public MessageQueuePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(MessageQueue.class).asEagerSingleton();
				bind(SteppableScheduler.class).asEagerSingleton();
				bind(MessageQueueEngine.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimListener>> listeners() {
		Collection<Class<? extends MobsimListener>> result = new ArrayList<>();
		result.add(MessageQueueEngine.class);
		return result;
	}
}
