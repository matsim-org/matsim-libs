package org.matsim.contrib.drt.extension.alonso_mora.travel_time;

import java.util.Iterator;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Verify;

/**
 * This implementation of the travel time estimator routes every request
 * exactly, based on the given departure time and current and future assumptions
 * on travel time provided by the TravelTime object.
 * 
 * Optionally, the results are cached for a configurable duration. During this
 * time, values are reused and cleaned up afterwards for any specific
 * origin-destination pair.
 * 
 * @author sebhoerl
 */
public class RoutingTravelTimeEstimator implements TravelTimeEstimator {
	static public final String TYPE = "Routing";
	
	static private final double purgeInterval = 300.0;
	double lastPurgeTime = Double.NEGATIVE_INFINITY;

	private final LeastCostPathCalculator router;
	private final TravelTime travelTime;

	private final MobsimTimer timer;

	private final IdMap<Link, IdMap<Link, Item>> cache = new IdMap<>(Link.class);
	private final double cacheLifetime;

	public RoutingTravelTimeEstimator(MobsimTimer mobsimTimer, LeastCostPathCalculator router, TravelTime travelTime,
			double cacheLifetime) {
		this.router = router;
		this.travelTime = travelTime;
		this.cacheLifetime = cacheLifetime;
		this.timer = mobsimTimer;
	}

	@Override
	public double estimateTravelTime(Link fromLink, Link toLink, double departureTime, double arrivalTimeThreshold) {
		if (cacheLifetime > 0.0) {
			if (timer.getTimeOfDay() > lastPurgeTime + purgeInterval) {
				for (IdMap<Link, Item> map : cache.values()) {
					Iterator<Item> iterator = map.values().iterator();

					while (iterator.hasNext()) {
						Item item = iterator.next();

						if (item.deletionTime <= timer.getTimeOfDay()) {
							iterator.remove();
						}
					}
				}

				Iterator<IdMap<Link, Item>> iterator = cache.values().iterator();

				while (iterator.hasNext()) {
					if (iterator.next().size() == 0) {
						iterator.remove();
					}
				}

				lastPurgeTime = timer.getTimeOfDay();
			}
		}

		if (cacheLifetime > 0.0) {
			synchronized (cache) {
				IdMap<Link, Item> cacheEntry = cache.get(fromLink.getId());

				if (cacheEntry != null) {
					Item cacheItem = cacheEntry.get(toLink.getId());

					if (cacheItem != null) {
						if (cacheItem.deletionTime <= timer.getTimeOfDay()) {
							cacheEntry.remove(toLink.getId());
						} else {
							Verify.verify(Double.isFinite(cacheItem.traveTime));
							return cacheItem.traveTime;
						}
					}
				}
			}
		}

		double value = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTime).getTravelTime();
		Verify.verify(Double.isFinite(value));

		if (cacheLifetime > 0.0) {
			synchronized (cache) {
				cache //
						.computeIfAbsent(fromLink.getId(), k -> new IdMap<>(Link.class)) //
						.put(toLink.getId(), new Item(value, timer.getTimeOfDay() + cacheLifetime));
			}
		}

		return value;
	}

	static public class Item {
		double deletionTime;
		double traveTime;

		Item(double travelTime, double deletionTime) {
			this.traveTime = travelTime;
			this.deletionTime = deletionTime;
		}
	}
}
