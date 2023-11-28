package org.matsim.contrib.drt.extension.estimator.run;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtEstimatorConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets implements Modal {

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

	public static final String GROUP_NAME = "drtEstimator";

	public DrtEstimatorConfigGroup() {
		super(GROUP_NAME);
	}

	public DrtEstimatorConfigGroup(String mode) {
		super(GROUP_NAME);
		this.mode = mode;
	}

	@Parameter
	@Comment("Mode of the drt service to estimate.")
	@NotBlank
	public String mode = TransportMode.drt;

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

	@Override
	public String getMode() {
		return mode;
	}

	/**
	 * Set estimator type and return same instance.
	 */
	public DrtEstimatorConfigGroup withEstimator(EstimatorType estimator) {
		this.estimator = estimator;
		return this;
	}

}
