package org.matsim.contrib.taxibus.routing;

import org.matsim.core.router.StageActivityTypes;

public class TaxibusStageActivityType implements StageActivityTypes {
	public static final String TAXIBUSTAGEACTIVITY = "taxibus interaction";

	@Override
	public boolean isStageActivity(String activityType) {
		return activityType.equals(TAXIBUSTAGEACTIVITY);
	}
}