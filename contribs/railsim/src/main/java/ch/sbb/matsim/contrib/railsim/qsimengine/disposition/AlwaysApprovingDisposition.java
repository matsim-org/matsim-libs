package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;

/**
 * This disposition assumes that the train is not blocked and always approves the next segment.
 * Should be used together with {@link ch.sbb.matsim.contrib.railsim.qsimengine.resources.NoopResourceManager}, which does not block links.
 */
public final class AlwaysApprovingDisposition implements TrainDisposition {

	private final RailResourceManager resources;
	private final SpeedProfile speedProfile;

	public AlwaysApprovingDisposition(RailResourceManager resources, SpeedProfile speedProfile) {
		this.resources = resources;
		this.speedProfile = speedProfile;
	}

	@Override
	public void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route) {
	}

	@Override
	public DispositionResponse requestNextSegment(double time, TrainPosition position, double dist) {

		RailLink currentLink = resources.getLink(position.getHeadLink());
		double reserveDist = currentLink.length - position.getHeadPosition();

		PlannedArrival nextArrival = speedProfile.getNextArrival(time, position);
		double approvedSpeed = speedProfile.getTargetSpeed(time, position, nextArrival);

		return new DispositionResponse(reserveDist, approvedSpeed, null);
	}

	@Override
	public void unblockRailLink(double time, MobsimDriverAgent driver, RailLink link) {

	}
}
