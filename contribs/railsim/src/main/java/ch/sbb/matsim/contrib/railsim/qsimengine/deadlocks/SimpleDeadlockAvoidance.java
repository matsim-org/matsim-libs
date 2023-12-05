package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import jakarta.inject.Inject;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

/**
 * A simple deadlock avoidance strategy that backtracks conflicting points in the schedule of trains.
 * This implementation does not guarantee deadlock freedom, but can avoid many cases.
 */
public class SimpleDeadlockAvoidance implements DeadlockAvoidance {



	@Inject
	public SimpleDeadlockAvoidance() {
	}

	@Override
	public void onReserve(double time, RailResource resource, TrainPosition position) {

	}

	@Override
	public boolean check(double time, RailResource resource, TrainPosition position) {

		for (RailLink link : position.getRouteUntilNextStop()) {

		}

		return false;
	}

	@Override
	public void onRelease(double time, RailResource resource, MobsimDriverAgent driver) {

	}

}
