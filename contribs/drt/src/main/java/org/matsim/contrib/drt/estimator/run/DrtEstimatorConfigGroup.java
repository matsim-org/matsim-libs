package org.matsim.contrib.drt.estimator.run;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.estimator.BasicDrtEstimator;
import org.matsim.contrib.drt.estimator.DrtEstimator;
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

	// default wait time
	// default detour

	// window
	// decay factor
	// randomisation

	@Override
	public String getMode() {
		return mode;
	}

}
