package org.matsim.contrib.drt.extension.shifts.config;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.drt.run.DrtConfigGroup;

public class DrtWithShiftsConfigGroup extends DrtConfigGroup {

	@NotNull
	private ShiftDrtConfigGroup drtShiftParams;

	public DrtWithShiftsConfigGroup() {
		addDefinition(ShiftDrtConfigGroup.GROUP_NAME, ShiftDrtConfigGroup::new,
				() -> drtShiftParams,
				params -> drtShiftParams = (ShiftDrtConfigGroup)params);
	}

	public ShiftDrtConfigGroup getDrtShiftParams() {
		return drtShiftParams;
	}

}
