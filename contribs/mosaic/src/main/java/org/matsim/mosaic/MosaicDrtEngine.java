package org.matsim.mosaic;

import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * Bridge between MATSim drt fleet and mosaic co-simulation framework.
 */
@SuppressWarnings("rawtypes")
public class MosaicDrtEngine implements DrtOptimizer, MobsimInitializedListener, MobsimBeforeCleanupListener {

	@Override
	public void requestSubmitted(Request request) {

	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {

	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {

	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {

	}
}
