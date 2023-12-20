package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;

/**
 * Does not prevent deadlocks and permits every request.
 */
public class NoDeadlockAvoidance implements DeadlockAvoidance {


	@Override
	public void onReserve(double time, RailResource resource, TrainPosition position) {
	}


	@Override
	public boolean checkLink(double time, RailLink link, TrainPosition position) {
		return true;
	}

	@Override
	public boolean checkReroute(double time, RailLink start, RailLink end, List<RailLink> subRoute, List<RailLink> detour, TrainPosition position) {
		return true;
	}

	@Override
	public void onRelease(double time, RailResource resource, MobsimDriverAgent driver) {
	}


}
