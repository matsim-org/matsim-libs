package org.matsim.core.mobsim.dsim;

import org.matsim.core.mobsim.qsim.ActivityEngine;

/**
 * Interface for handling activities of agents.
 * This interface indicates that the activity handler is safe for use in distributed simulations.
 *
 * @see ActivityEngine
 */
public interface DistributedActivityEngine extends ActivityEngine, DistributedActivityHandler, DistributedMobsimEngine {

}
