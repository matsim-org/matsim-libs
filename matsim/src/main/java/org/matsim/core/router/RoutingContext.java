package org.matsim.core.router;

import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public interface RoutingContext {

	TravelDisutility getTravelDisutility();

	TravelTime getTravelTime();

}
