package org.matsim.core.mobsim.disim;

import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.qsim.InternalInterface;

/**
 * A distributed engine accepts agents and processes simulation steps.
 */
public interface DistributedMobsimEngine extends Steppable {

	/**
	 * Process a person.
	 *
	 * @param person person to accept
	 * @param now    current simulation time
	 */
	void accept(DistributedMobsimAgent person, double now);

	void process(SimStepMessage stepMessage, double now);

	void setInternalInterface(InternalInterface internalInterface);
}
