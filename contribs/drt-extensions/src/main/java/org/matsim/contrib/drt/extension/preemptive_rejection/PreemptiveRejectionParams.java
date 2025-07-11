package org.matsim.contrib.drt.extension.preemptive_rejection;

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class PreemptiveRejectionParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
	public static final String SET_NAME = "preemptiveRejection";

	@Parameter
	private String inputPath;

	public PreemptiveRejectionParams() {
		super(SET_NAME);
	}

	public String getInputPath() {
		return inputPath;
	}

	public void setInputPath(String val) {
		inputPath = val;
	}
}
