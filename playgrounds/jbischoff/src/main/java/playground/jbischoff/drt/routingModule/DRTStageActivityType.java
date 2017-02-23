package playground.jbischoff.drt.routingModule;

import org.matsim.core.router.StageActivityTypes;

public class DRTStageActivityType implements StageActivityTypes
{
	public static final String DRTSTAGEACTIVITY = "DRT interaction";
	
	@Override
	public boolean isStageActivity(String activityType) {
		return activityType.equals(DRTSTAGEACTIVITY);
	}
	
}