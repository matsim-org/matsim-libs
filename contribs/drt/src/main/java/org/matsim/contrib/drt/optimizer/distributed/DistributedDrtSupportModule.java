package org.matsim.contrib.drt.optimizer.distributed;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

/**
 * Contains functionality needed in a distributed setup.
 */
public class DistributedDrtSupportModule extends AbstractModule {

	@Override
	public void install() {

		bind(DrtNodeCommunicator.class).in(Singleton.class);

		addMobsimListenerBinding().to(DrtNodeCommunicator.class).in(Singleton.class);

	}

}
