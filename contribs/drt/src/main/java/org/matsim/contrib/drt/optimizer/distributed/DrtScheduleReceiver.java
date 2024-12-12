package org.matsim.contrib.drt.optimizer.distributed;

import org.matsim.core.mobsim.dsim.NodeSingleton;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

/**
 * This component receives the {@link org.matsim.contrib.dvrp.schedule.Schedule} from the head node.
 */
@NodeSingleton
public class DrtScheduleReceiver implements MobsimAfterSimStepListener {

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {


	}

}
