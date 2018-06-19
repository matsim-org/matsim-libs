package org.matsim.contrib.drt.routing;

import org.matsim.core.router.StageActivityTypes;

public class DrtStageActivityType implements StageActivityTypes {
	public static final String DRT_STAGE_ACTIVITY = "drt interaction";
	public static final String DRT_WALK = "drt_walk";

	@Override
	public boolean isStageActivity(String activityType) {
		return activityType.equals(DRT_STAGE_ACTIVITY);
	}
}