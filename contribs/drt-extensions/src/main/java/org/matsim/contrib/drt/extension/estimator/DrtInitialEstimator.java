package org.matsim.contrib.drt.extension.estimator;

/**
 * This interface is used to provide an initial estimate for the drt service.
 * Supposed to be used when no data is available from the simulation yet.
 * The interface is exactly the same as {@link DrtEstimator}, but this class won't be called with update events.
 */
public interface DrtInitialEstimator extends DrtEstimator {
}
