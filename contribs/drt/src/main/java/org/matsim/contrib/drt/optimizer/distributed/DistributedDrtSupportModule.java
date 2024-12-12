package org.matsim.contrib.drt.optimizer.distributed;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

/**
 * Contains functionality needed in a distributed setup.
 */
public class DistributedDrtSupportModule extends AbstractModule {

	@Override
	public void install() {

		if (getSimulationNode().isHeadNode()) {

			addMobsimListenerBinding().to(DrtHeadNodeCommunicator.class).in(Singleton.class);

		} else {

			addMobsimListenerBinding().to(DrtScheduleReceiver.class).in(Singleton.class);

		}

	}

}
