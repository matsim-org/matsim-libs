package org.matsim.contrib.drt.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.optimizer.insertion.filter.DrtVehicleFilter;
import org.matsim.contrib.drt.scheduler.DrtScheduler;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

/**
 * @author michalm
 */
public class DrtOptimizerContext {
	public final Fleet fleet;
	public final Network network;
	public final MobsimTimer timer;
	public final TravelTime travelTime;
	public final TravelDisutility travelDisutility;
	public final DrtScheduler scheduler;
	public final EventsManager eventsManager;
	public final DrtVehicleFilter vehicleFilter;
	public final DrtRequestValidator requestValidator;

	public DrtOptimizerContext(Fleet fleet, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, DrtScheduler scheduler, EventsManager eventsManager,
			DrtVehicleFilter filter, DrtRequestValidator validator) {
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;
		this.eventsManager = eventsManager;
		this.vehicleFilter = filter;
		this.requestValidator = validator;
	}
}
