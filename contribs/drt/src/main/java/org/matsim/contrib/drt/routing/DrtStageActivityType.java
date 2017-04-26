package org.matsim.contrib.drt.routing;

import org.matsim.core.router.StageActivityTypes;

public class DrtStageActivityType implements StageActivityTypes {
	public static final String DRTSTAGEACTIVITY = "drt interaction";

	@Override
	public boolean isStageActivity(String activityType) {
		return activityType.equals(DRTSTAGEACTIVITY);
	}
}