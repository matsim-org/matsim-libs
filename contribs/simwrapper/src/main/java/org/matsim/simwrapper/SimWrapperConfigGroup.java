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
	private Mode defaultDashboards = Mode.enabled;

	@Parameter
	@Comment("Set of packages to scan for dashboard provider classes.")
	private Set<String> packages = new HashSet<>();

	@Parameter
	@Comment("Set of simple class names or fully qualified class names of dashboards to exclude")
	private Set<String> exclude = new HashSet<>();

	@Parameter
	@Comment("Set of simple class names or fully qualified class names of dashboards to include. Any none included dashboard will be excluded.")
	private Set<String> include = new HashSet<>();

	@Parameter
	@Comment("Sample size of the run, which may be required by certain analysis functions.")
	private Double sampleSize = 1.0d;

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
			p.setContext(context);
			if (!context.isEmpty()) {
				// Copy default params from the global config
				p.setShp(defaultParams().getShp());
				p.setMapCenter(defaultParams().getMapCenter());
				p.setMapZoomLevel(defaultParams().getMapZoomLevel());
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
			params.put(ctx.getContext(), ctx);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (!getInclude().isEmpty() && !getExclude().isEmpty()) {
			throw new IllegalStateException("Include and exclude option can't be set both.");
		}
	}

	public Mode getDefaultDashboards() {
		return defaultDashboards;
	}

	public void setDefaultDashboards(Mode defaultDashboards) {
		this.defaultDashboards = defaultDashboards;
	}

	public Set<String> getPackages() {
		return packages;
	}

	public void setPackages(Set<String> packages) {
		this.packages = packages;
	}

	public Set<String> getExclude() {
		return exclude;
	}

	public void setExclude(Set<String> exclude) {
		this.exclude = exclude;
	}

	public Set<String> getInclude() {
		return include;
	}

	public void setInclude(Set<String> include) {
		this.include = include;
	}

	public Double getSampleSize() {
		return sampleSize;
	}

	public void setSampleSize(Double sampleSize) {
		this.sampleSize = sampleSize;
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
		private String context = "";

		@Parameter
		@Comment("Shp file that may be used by analysis functions that support shp file input.")
		private String shp = null;

		@Parameter
		@Comment("Tuple of two coordinate separated with ',' that may be used to define the center of map views.")
		private String mapCenter = null;

		@Parameter
		@Comment("Default zoom level used for map view.")
		private Double mapZoomLevel = null;

		public ContextParams() {
			super(GROUP_NAME, true);
		}

		/**
		 * Return center coordinates, or null if not set.
		 */
		public double[] getCenter() {
			if (getMapCenter() == null || !getMapCenter().contains(","))
				return null;

			return Arrays.stream(getMapCenter().split(",")).mapToDouble(Double::parseDouble).toArray();
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

		public String getContext() {
			return context;
		}

		public void setContext(String context) {
			this.context = context;
		}

		public String getShp() {
			return shp;
		}

		public void setShp(String shp) {
			this.shp = shp;
		}

		public String getMapCenter() {
			return mapCenter;
		}

		public void setMapCenter(String mapCenter) {
			this.mapCenter = mapCenter;
		}

		public Double getMapZoomLevel() {
			return mapZoomLevel;
		}

		public void setMapZoomLevel(Double mapZoomLevel) {
			this.mapZoomLevel = mapZoomLevel;
		}
	}

}
