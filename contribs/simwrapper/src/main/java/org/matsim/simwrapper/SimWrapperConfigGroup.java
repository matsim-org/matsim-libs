package org.matsim.simwrapper;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.*;

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

	@Parameter
	@Comment("Set of simple class names or fully qualified class names of dashboards to include. Any none included dashboard will be excluded.")
	public Set<String> include = new HashSet<>();

	@Parameter
	@Comment("Sample size of the run, which may be required by certain analysis functions.")
	public Double sampleSize = 1.0d;

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
			p.context = context;
			if (!context.isEmpty()) {
				// Copy default params from the global config
				p.shp = defaultParams().shp;
				p.mapCenter = defaultParams().mapCenter;
				p.mapZoomLevel = defaultParams().mapZoomLevel;
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
			params.put(ctx.context, ctx);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (!include.isEmpty() && !exclude.isEmpty()) {
			throw new IllegalStateException("Include and exclude option can't be set both.");
		}
	}

	/**
	 * Mode how default dashboards are loaded.
	 */
	public enum Mode {
		enabled,
		disabled
	}

	/**
	 * Stores context specific parameters.
	 */
	public static final class ContextParams extends ReflectiveConfigGroup {
		private static final String GROUP_NAME = "params";

		@Parameter
		@Comment("Name of the context, empty string means default context.")
		public String context = "";

		@Parameter
		@Comment("Shp file that may be used by analysis functions that support shp file input.")
		public String shp = null;

		@Parameter
		@Comment("Tuple of two coordinate separated with ',' that may be used to define the center of map views.")
		public String mapCenter = null;

		@Parameter
		@Comment("Default zoom level used for map view.")
		public Double mapZoomLevel = null;

		public ContextParams() {
			super(GROUP_NAME, true);
		}

		/**
		 * Return center coordinates, or null if not set.
		 */
		public double[] getCenter() {
			if (mapCenter == null || !mapCenter.contains(","))
				return null;

			return Arrays.stream(mapCenter.split(",")).mapToDouble(Double::parseDouble).toArray();
		}

		/**
		 * Return an arbitrary config parameter that has been stored, or the default value if it is not present.
		 */
		public String getOrDefault(String name, String def) {
			return getParams().getOrDefault(name, def);
		}

		/**
		 * Sets an arbitrary config value.
		 *
		 * @return same instance.
		 */
		public ContextParams set(String key, String value) {
			super.getParams().put(key, value);
			return this;
		}
	}

}
