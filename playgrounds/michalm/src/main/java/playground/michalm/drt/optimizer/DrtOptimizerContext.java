package playground.michalm.drt.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

import playground.michalm.drt.scheduler.DrtScheduler;

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

	public DrtOptimizerContext(Fleet fleet, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, DrtScheduler scheduler) {
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;
	}
}
