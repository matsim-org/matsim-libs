package org.matsim.contrib.drt.optimizer;

import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

import java.util.concurrent.ForkJoinPool;

/**
 * @author steffenaxer
 */
public interface QsimScopeForkJoinPool extends MobsimBeforeCleanupListener {
	ForkJoinPool getPool();
}
