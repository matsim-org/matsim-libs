package org.matsim.contrib.drt.routing;

import java.util.Objects;

public class DrtStageActivityType {
	public final String drtStageActivity;
	public final String drtWalk;

	public DrtStageActivityType(String drtMode) {
		drtStageActivity = drtMode + " interaction";
		drtWalk = drtMode + "_walk";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DrtStageActivityType that = (DrtStageActivityType)o;
		return Objects.equals(drtWalk, that.drtWalk);
	}

	@Override
	public int hashCode() {
		return Objects.hash(drtStageActivity, drtWalk);
	}
}
