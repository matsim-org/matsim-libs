package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;

/**
 * A moving block, which allows multiple trains and needs to make sure that they don't collide.
 */
public final class MovingBlockResource implements RailResource {

	final List<RailLink> links;

	public MovingBlockResource(List<RailLink> links) {
		this.links = links;
	}

	@Override
	public List<RailLink> getLinks() {
		return links;
	}

	@Override
	public boolean hasCapacity(double time, TrainPosition position) {
		return false;
	}

	@Override
	public boolean isReservedBy(MobsimDriverAgent driver) {
		return false;
	}

	@Override
	public void reserve(TrainPosition position) {

	}

	@Override
	public void release(MobsimDriverAgent driver) {

	}
}
