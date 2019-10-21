package org.matsim.contrib.drt.routing;

import java.util.Objects;

public class DrtStageActivityType {
	public final String drtStageActivity;

	public DrtStageActivityType(String drtMode) {
		drtStageActivity = drtMode + " interaction";
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
		return Objects.equals(drtStageActivity, that.drtStageActivity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(drtStageActivity);
	}
}
