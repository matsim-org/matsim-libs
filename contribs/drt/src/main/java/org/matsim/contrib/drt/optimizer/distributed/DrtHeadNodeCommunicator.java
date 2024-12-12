package org.matsim.contrib.drt.optimizer.distributed;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.core.mobsim.dsim.NodeSingleton;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.dsim.MessageBroker;

/**
 * Class running on the head node to receive requests and send out schedules.
 */
@NodeSingleton
public class DrtHeadNodeCommunicator implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener {

	private final MessageBroker broker;

//	private Map<String, List<>> optimizers;

	@Inject
	public DrtHeadNodeCommunicator(Injector injector) {
		this.broker = injector.getInstance(MessageBroker.class);

		for (Binding<?> binding : injector.getAllBindings().values()) {

			Class<?> type = binding.getKey().getTypeLiteral().getRawType();
			if (DrtOptimizer.class.isAssignableFrom(type)) {
				// TODO: find optimizer which receive the requests
//				optimizers.add((DrtOptimizer) injector.getInstance(type));
			}
		}

	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		// TODO: send vehicle schedules, but after optimizer finished

	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		broker.receiveNodeMessages(RequestMessage.class, this::handleMessage);
	}

	private void handleMessage(RequestMessage message) {

		System.out.println(message);

	}

}
