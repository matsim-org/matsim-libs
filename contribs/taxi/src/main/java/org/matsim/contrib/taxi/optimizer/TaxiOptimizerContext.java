package org.matsim.contrib.taxi.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

public class TaxiOptimizerContext {
	public final Fleet fleet;
	public final Network network;
	public final MobsimTimer timer;
	public final TravelTime travelTime;
	public final TravelDisutility travelDisutility;
	public final TaxiScheduler scheduler;

	public TaxiOptimizerContext(Fleet fleet, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, TaxiScheduler scheduler) {
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;
	}
}
