package org.matsim.contrib.drt.extension.shifts.config;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.drt.run.DrtConfigGroup;

public class DrtWithShiftsConfigGroup extends DrtConfigGroup {

	@NotNull
	private DrtShiftParams drtShiftParams;

	public DrtWithShiftsConfigGroup() {
		addDefinition(DrtShiftParams.SET_NAME, DrtShiftParams::new,
				() -> drtShiftParams,
				params -> drtShiftParams = (DrtShiftParams)params);
	}

	public DrtShiftParams getDrtShiftParams() {
		return drtShiftParams;
	}

}
