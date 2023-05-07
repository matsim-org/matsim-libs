package org.matsim.simwrapper;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Config group for the {@link SimWrapperModule}.
 */
public class SimWrapperConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "simwrapper";
	/**
	 * Stores context configs.
	 */
	private final Map<String, ContextParams> params = new HashMap<>();

	@Parameter
	@Comment("Whether default dashboards are loaded via SPI.")
	public Mode defaultDashboards = Mode.enabled;
	@Parameter
	@Comment("Set of packages to scan for dashboard provider classes.")
	public Set<String> packages = new HashSet<>();
	@Parameter
	@Comment("Set of simple class names or fully qualified class names of dashboards to exclude")
	public Set<String> exclude = new HashSet<>();

	public SimWrapperConfigGroup() {
		super(NAME);
		get("");
	}

	/**
	 * Get the default parameters that are always present in the config.
	 */
	public ContextParams defaultParams() {
		return get("");
	}

	/**
	 * Get an existing or add a new parameter set for specific context.
	 */
	public ContextParams get(String context) {
		if (!params.containsKey(context)) {
			ContextParams p = new ContextParams();
			p.name = context;
			if (!context.equals("")) {
				// Copy default params from the global config
				p.sampleSize = defaultParams().sampleSize;
			}
			addParameterSet(p);
			return p;
		}

		return params.get(context);
	}

	@Override
	public ContextParams createParameterSet(String type) {
		if (type.equals(ContextParams.GROUP_NAME)) {
			return new ContextParams();
		} else {
			throw new IllegalArgumentException("Unsupported parameter set type: " + type);
		}
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof ContextParams ctx) {
			super.addParameterSet(set);
			params.put(ctx.name, ctx);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
	}

	/**
	 * Mode how how default dashboards are loaded.
	 */
	public enum Mode {
		enabled,
		disabled
	}

	/**
	 * Stores context specific parameters.
	 */
	public static final class ContextParams extends ReflectiveConfigGroup {
		private static final String GROUP_NAME = "context";

		@Parameter
		@Comment("Name of the context, empty string means default context.")
		public String name = "";

		@Parameter
		@Comment("Sample size of the run, which may be required by certain analysis functions.")
		public String sampleSize = "1.0";

		// For map:
		// TODO: zoomLevel
		// TODO: center
		// default crs

		// TODO: a config set with different shp files
		// TODO: config set with just some key value parameters

		public ContextParams() {
			super(GROUP_NAME, true);
		}
	}

}
