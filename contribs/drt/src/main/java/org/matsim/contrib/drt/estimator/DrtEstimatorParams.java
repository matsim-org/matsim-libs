package org.matsim.contrib.drt.estimator;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.ReflectiveConfigGroup;

public class DrtEstimatorParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "estimator";

	public DrtEstimatorParams() {
		super(SET_NAME);
	}


	@Parameter
	@Comment("Decay of the exponential moving average.")
	@Positive
	public double decayFactor = 0.7;

	@Parameter
	@Comment("Factor multiplied with standard deviation to randomize estimates.")
	@PositiveOrZero
	public double randomization = 0.1;


}
