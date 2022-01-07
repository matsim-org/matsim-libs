package org.matsim.contrib.drt.extension.alonso_mora.travel_time;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

import com.google.inject.Singleton;

/**
 * This is a custom implementation of the TravelTimeEstimator which allows to
 * add a constant or linear bias to the values estimated by another estimator.
 * It's main use is to perform parametric studies for checking the influence of
 * estimation bias on the matching. It can be used in overriding existing
 * estimators in custom simulation set-ups.
 * 
 * @author sebhoerl
 */
public class BiasedEstimator implements TravelTimeEstimator {
	private final TravelTimeEstimator delegate;

	private final double linearBias;
	private final double constantBias;

	public BiasedEstimator(TravelTimeEstimator delegate, double linearBias, double constantBias) {
		this.linearBias = linearBias;
		this.constantBias = constantBias;
		this.delegate = delegate;
	}

	@Override
	public double estimateTravelTime(Link fromLink, Link toLink, double departureTime, double arrivalTimeThreshold) {
		return constantBias
				+ delegate.estimateTravelTime(fromLink, toLink, departureTime, arrivalTimeThreshold) * linearBias;
	}

	static public AbstractDvrpModeQSimModule wrap(String mode, double linearBias, double constantBias,
			Class<? extends TravelTimeEstimator> implementation) {
		return new AbstractDvrpModeQSimModule(mode) {
			@Override
			protected void configureQSim() {
				bindModal(BiasedEstimator.class).toProvider(modalProvider(getter -> {
					TravelTimeEstimator delegate = getter.getModal(implementation);
					return new BiasedEstimator(delegate, linearBias, constantBias);
				})).in(Singleton.class);

				bindModal(TravelTimeEstimator.class).to(modalKey(BiasedEstimator.class));
			}
		};
	}
}
