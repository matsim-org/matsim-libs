package org.matsim.contrib.drt.routing;

import org.matsim.core.router.StageActivityTypes;

public class DrtStageActivityType implements StageActivityTypes {
	public final String drtStageActivity;
	public final String drtWalk;
	
	public DrtStageActivityType (String drtMode) {
		drtStageActivity = drtMode + " interaction";
		drtWalk = drtMode + "_walk";
	}

	@Override
	public boolean isStageActivity(String activityType) {
		return activityType.equals(drtStageActivity);
	}
}
