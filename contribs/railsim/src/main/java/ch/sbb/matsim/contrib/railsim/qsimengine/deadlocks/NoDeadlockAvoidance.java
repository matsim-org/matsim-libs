package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Does not prevent deadlocks and permits every request.
 */
public class NoDeadlockAvoidance implements DeadlockAvoidance {


	@Override
	public void onReserve(double time, RailResource resource, TrainPosition position) {
	}

	@Nullable
	@Override
	public RailLink checkSegment(double time, List<RailLink> segment, TrainPosition position) {
		return null;
	}

	@Override
	public void onRelease(double time, RailResource resource, MobsimDriverAgent driver) {
	}


}
