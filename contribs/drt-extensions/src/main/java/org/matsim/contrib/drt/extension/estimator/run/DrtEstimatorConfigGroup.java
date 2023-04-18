package org.matsim.contrib.drt.extension.estimator.run;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.extension.estimator.BasicDrtEstimator;
import org.matsim.contrib.drt.extension.estimator.DrtEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtEstimatorConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets implements Modal {

	private static final Logger log = LogManager.getLogger(DrtConfigGroup.class);

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
	@Comment("Fully qualified class name of the estimator that should be used.")
	@NotNull
	public Class<? extends DrtEstimator> estimator = BasicDrtEstimator.class;

	@Parameter
	@Comment("Decay of the exponential moving average.")
	@Positive
	public double decayFactor = 0.5;

	@Parameter
	@Comment("Randomize estimates with standard deviation")
	@PositiveOrZero
	public double randomization = 0.1;

	@Parameter
	@Comment("Default wait time in seconds, when no estimates are present.")
	@PositiveOrZero
	public double defaultWaitTime = 300;

	@Parameter
	@Comment("Default detour factor, when no estimates are present.")
	@PositiveOrZero
	public double defaultDetourFactor = 1.05;


	@Override
	public String getMode() {
		return mode;
	}

}
