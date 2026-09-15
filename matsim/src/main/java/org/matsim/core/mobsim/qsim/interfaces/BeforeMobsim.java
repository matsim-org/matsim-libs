package org.matsim.core.mobsim.qsim.interfaces;

/**
 * Lifecycle hook for components that need to perform actions before the mobsim starts.
 */
public interface BeforeMobsim {

	/**
	 * Called before the first timestep of the mobsim is executed, but after the mobsim is initialized.
	 */
	void beforeMobsim();
}
