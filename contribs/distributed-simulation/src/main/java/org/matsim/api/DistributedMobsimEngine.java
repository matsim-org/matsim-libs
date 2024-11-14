package org.matsim.api;

import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.dsim.messages.SimStepMessage;

/**
 * A distributed engine accepts agents and processes simulation steps.
 */
public interface DistributedMobsimEngine extends Steppable {

	/**
	 * Process a person.
	 * @param person person to accept
	 * @param now    current simulation time
	 */
	void accept(DistributedMobsimAgent person, double now);

	void process(SimStepMessage stepMessage, double now);

	void setInternalInterface(InternalInterface internalInterface);
}
