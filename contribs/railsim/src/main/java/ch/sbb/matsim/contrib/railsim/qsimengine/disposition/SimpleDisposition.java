package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimTransitDriverAgent;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import jakarta.inject.Inject;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple disposition without deadlock avoidance.
 */
public class SimpleDisposition implements TrainDisposition {

	private final RailResourceManager resources;
	private final TrainRouter router;

	@Inject
	public SimpleDisposition(RailResourceManager resources, TrainRouter router) {
		this.resources = resources;
		this.router = router;
	}

	@Override
	public void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route) {
		// Nothing to do.
	}

	@Nullable
	@Override
	public List<RailLink> requestRoute(double time, RailsimTransitDriverAgent driver, List<RailLink> segment, RailLink entry, RailLink exit) {

		// Only re-routes if the link segment is occupied
		for (RailLink link : segment) {
			if (!resources.isBlockedBy(link, driver) && !resources.hasCapacity(link.getLinkId()))
				return router.calcRoute(entry, exit);
		}

		return null;
	}

	@Override
	public List<RailLink> blockRailSegment(double time, MobsimDriverAgent driver, List<RailLink> segment) {

		List<RailLink> blocked = new ArrayList<>();

		// Iterate all links that need to be blocked
		for (RailLink link : segment) {

			// Check if single link can be reserved
			if (resources.tryBlockTrack(time, driver, link)) {
				blocked.add(link);
			} else
				break;
		}

		return blocked;
	}

	@Override
	public void unblockRailLink(double time, MobsimDriverAgent driver, RailLink link) {

		// put resource handling into release track
		resources.releaseTrack(time, driver, link);
	}
}
