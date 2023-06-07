package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

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
