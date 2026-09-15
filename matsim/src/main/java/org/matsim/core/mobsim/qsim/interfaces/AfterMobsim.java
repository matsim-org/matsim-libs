package org.matsim.core.mobsim.qsim.interfaces;

/**
 * Lifecycle hook for components that need to perform actions after the mobsim has finished.
 */
public interface AfterMobsim {

	/**
	 * Called after the last timestep of the mobsim is executed.
	 */
	void afterMobsim();
}
