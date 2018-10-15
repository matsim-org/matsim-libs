package org.matsim.contrib.drt.routing;

import org.matsim.core.router.StageActivityTypes;

public class DrtStageActivityType implements StageActivityTypes {
	public final String DRT_STAGE_ACTIVITY;
	public final String DRT_WALK;
	
	public DrtStageActivityType (String drtMode) {
		DRT_STAGE_ACTIVITY = drtMode + " interaction";
		DRT_WALK = drtMode + "_walk";
	}

	@Override
	public boolean isStageActivity(String activityType) {
		return activityType.equals(DRT_STAGE_ACTIVITY);
	}
}