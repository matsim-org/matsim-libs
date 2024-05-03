package org.matsim.contrib.common.zones.systems.grid;

import com.google.common.base.Verify;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.core.config.Config;

import javax.annotation.Nullable;

/**
 * @author nkuehnel / MOIA
 */
public class GISFileZoneSystemParams extends ZoneSystemParams {

	public static final String SET_NAME = "GISFileZoneSystem";

	public GISFileZoneSystemParams() {
		super(SET_NAME);
	}

	@Parameter
	@Comment("allows to configure zones. Used with zonesGeneration=ShapeFile")
	@Nullable
	public String zonesShapeFile = null;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Verify.verify(zonesShapeFile != null, "GIS zone input file must not be null.");
	}
}
