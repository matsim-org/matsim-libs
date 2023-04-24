package org.matsim.simwrapper;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Config group for the {@link SimWrapperModule}.
 */
public class SimWrapperConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "simwrapper";

	@Parameter
	@Comment("Whether default dashboards should be generated.")
	private Mode defaultDashboards = Mode.enabled;

	public SimWrapperConfigGroup() {
		super(NAME);
	}

	public enum Mode {

		enabled,
		disabled

	}

}
