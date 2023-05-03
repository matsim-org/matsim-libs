package org.matsim.simwrapper;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Config group for the {@link SimWrapperModule}.
 */
public class SimWrapperConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "simwrapper";

	@Parameter
	@Comment("Whether default dashboards should be generated.")
	public Mode defaultDashboards = Mode.enabled;

	@Parameter
	@Comment("Sample size of the run, which may be required by certain analysis functions.")
	public String sampleSize = "1.0";

	// For map:
	// TODO: zoomLevel
	// TODO: center
	// default crs

	public SimWrapperConfigGroup() {
		super(NAME);
	}

	public enum Mode {

		enabled,
		disabled

	}

}
