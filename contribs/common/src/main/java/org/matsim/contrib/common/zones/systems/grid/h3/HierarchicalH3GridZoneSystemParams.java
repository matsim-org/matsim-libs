package org.matsim.contrib.common.zones.systems.grid.h3;

import com.google.common.base.Verify;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.core.config.Config;

import jakarta.annotation.Nullable;

/**
 * @author nkuehnel / MOIA
 */
public class HierarchicalH3GridZoneSystemParams extends ZoneSystemParams {

	public static final String SET_NAME = "HierarchicalH3GridZoneSystem";

	public HierarchicalH3GridZoneSystemParams() {
		super(SET_NAME);
	}

	@Parameter
	@Comment("Minimum (coarsest) H3 resolution. Range 0-15.")
	@Nullable
	private Integer h3MinResolution = null;

	@Parameter
	@Comment("Maximum (finest) H3 resolution. Range 0-15. Must be >= h3MinResolution.")
	@Nullable
	private Integer h3MaxResolution = null;

	@Parameter
	@Comment("Maximum number of network nodes per zone before subdivision.")
	@Nullable
	private Integer maxNodesPerZone = null;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(h3MinResolution != null && h3MinResolution >= 0 && h3MinResolution < 15,
				"H3 min resolution must be between 0 and 15.");
		Verify.verify(h3MaxResolution != null && h3MaxResolution >= h3MinResolution && h3MaxResolution < 15,
				"H3 max resolution must be between h3MinResolution and 15.");
		Verify.verify(maxNodesPerZone != null && maxNodesPerZone > 0,
				"maxNodesPerZone must be positive.");
	}

	@Nullable
	public Integer getH3MinResolution() {
		return h3MinResolution;
	}

	public void setH3MinResolution(@Nullable Integer h3MinResolution) {
		this.h3MinResolution = h3MinResolution;
	}

	@Nullable
	public Integer getH3MaxResolution() {
		return h3MaxResolution;
	}

	public void setH3MaxResolution(@Nullable Integer h3MaxResolution) {
		this.h3MaxResolution = h3MaxResolution;
	}

	@Nullable
	public Integer getMaxNodesPerZone() {
		return maxNodesPerZone;
	}

	public void setMaxNodesPerZone(@Nullable Integer maxNodesPerZone) {
		this.maxNodesPerZone = maxNodesPerZone;
	}
}