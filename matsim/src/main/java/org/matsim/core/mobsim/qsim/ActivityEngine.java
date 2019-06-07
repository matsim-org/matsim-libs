package org.matsim.core.mobsim.qsim;

import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

/**
 *  An interface that marks the "default" activity engine.
 */
public interface ActivityEngine extends ActivityHandler, MobsimEngine {
	//(default)ActivityEngine is very similar to TeleportationHandler (both are last handlers)
	// The name should more clearly express that intend.
	// Suggested names: NopActivityEngine, SleepActivityEngine, IdleActivityEngine, StaticActivityEngine???
	// michalm, mar '19

}
