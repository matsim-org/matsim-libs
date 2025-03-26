package org.matsim.contrib.common.zones.systems.grid.square;

import com.google.common.base.Verify;
import jakarta.validation.constraints.Positive;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.core.config.Config;

/**
 * @author nkuehnel / MOIA
 */
public class SquareGridZoneSystemParams extends ZoneSystemParams {

	public static final String SET_NAME = "SquareGridZoneSystem";

	public SquareGridZoneSystemParams() {
		super(SET_NAME);
	}

	@Parameter
	@Comment("size of square cells used for demand aggregation."
		+ " Depends on demand, supply and network. Often used with values in the range of 500 - 2000 m")
	@Positive
	private double cellSize = 200.;// [m]

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(getCellSize() > 0 && Double.isFinite(getCellSize()), "cell size must be finite and positive.");
	}

	@Positive
	public double getCellSize() {
		return cellSize;
	}

	public void setCellSize(@Positive double cellSize) {
		this.cellSize = cellSize;
	}
}
