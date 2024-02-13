package org.matsim.contrib.drt.estimator;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.ReflectiveConfigGroup;

public class DrtEstimatorParams extends ReflectiveConfigGroup {

	/**
	 * Type of estimator, which will be installed in {@link DrtEstimatorModule}.
	 */
	public enum EstimatorType {
		BASIC,

		/**
		 * Will use the bound initial estimator, without any updates.
		 */
		INITIAL,

		/**
		 * Custom estimator, that needs to provided via binding.
		 */
		CUSTOM
	}

	public static final String SET_NAME = "estimator";

	public DrtEstimatorParams() {
		super(SET_NAME);
	}

	@Parameter
	@Comment("Estimator typed to be used. In case of 'CUSTOM', guice bindings needs to be provided.")
	@NotNull
	public EstimatorType estimator = EstimatorType.BASIC;

	@Parameter
	@Comment("Decay of the exponential moving average.")
	@Positive
	public double decayFactor = 0.7;

	@Parameter
	@Comment("Factor multiplied with standard deviation to randomize estimates.")
	@PositiveOrZero
	public double randomization = 0.1;

	// TODO think about enum, or different place / name for this option
	@Parameter
	@Comment("Whether drt passengers should be teleported based on estimation")
	@Deprecated
	public boolean teleport = false;

	/**
	 * Set estimator type and return same instance.
	 */
	public DrtEstimatorParams withEstimator(EstimatorType estimator) {
		this.estimator = estimator;
		return this;
	}

}
