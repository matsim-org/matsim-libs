package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

/**
 * Interface for handling departures of agents.
 * This interface indicates that the departure handler is safe for use in distributed simulations.
 */
public interface DistributedDepartureHandler extends DepartureHandler {

	/**
	 * Engines are sorted by their priority in descending order.
	 */
	default double priority() {
		return 0.0;
	}

}
