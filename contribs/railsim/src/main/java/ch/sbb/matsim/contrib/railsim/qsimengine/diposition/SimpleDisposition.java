package ch.sbb.matsim.contrib.railsim.qsimengine.diposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailResource;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailResourceManager;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple disposition without deadlock avoidance.
 */
public class SimpleDisposition implements TrainDisposition {

	private final RailResourceManager resources;

	public SimpleDisposition(RailResourceManager resources) {
		this.resources = resources;
	}

	@Override
	public void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route) {
		// Nothing to do.
	}

	@Override
	public List<RailLink> blockRailSegment(double time, MobsimDriverAgent driver, List<RailLink> segment) {

		List<RailLink> blocked = new ArrayList<>();

		// Iterate all links that need to be blocked
		for (RailLink link : segment) {

			if (link.isBlockedBy(driver)) {
				blocked.add(link);
				continue;
			}

			Id<RailResource> resourceId = link.getResourceId();
			if (resourceId != null) {

				RailResource resource = resources.getResource(resourceId);
				if (resources.tryBlockResource(resource, driver)) {

					boolean b = resources.tryBlockTrack(time, driver, link);
					assert b : "Link blocked by resource must be free";

					blocked.add(link);
					continue;
				}

				// Could not reserve resource
				break;
			}

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
		resources.releaseTrack(time, driver, link);

		// Release held resources
		if (link.getResourceId() != null) {
			resources.tryReleaseResource(resources.getResource(link.getResourceId()), driver);
		}
	}
}
