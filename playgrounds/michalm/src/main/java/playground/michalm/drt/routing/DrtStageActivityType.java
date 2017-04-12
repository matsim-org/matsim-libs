package playground.michalm.drt.routing;

import org.matsim.core.router.StageActivityTypes;

public class DrtStageActivityType implements StageActivityTypes {
	public static final String DRTSTAGEACTIVITY = "DRT interaction";

	@Override
	public boolean isStageActivity(String activityType) {
		return activityType.equals(DRTSTAGEACTIVITY);
	}
}