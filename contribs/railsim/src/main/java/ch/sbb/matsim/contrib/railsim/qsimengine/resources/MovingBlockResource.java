package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;

/**
 * A moving block, which allows multiple trains and needs to make sure that they don't collide.
 */
final class MovingBlockResource implements RailResourceInternal {

	private final Id<RailResource> id;
	final List<RailLink> links;

	MovingBlockResource(Id<RailResource> id, List<RailLink> links) {
		this.id = id;
		this.links = links;
	}

	@Override
	public ResourceType getType() {
		return ResourceType.movingBlock;
	}

	@Override
	public List<RailLink> getLinks() {
		return links;
	}

	@Override
	public ResourceState getState(RailLink link) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasCapacity(RailLink link, TrainPosition position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getReservedDist(RailLink link, TrainPosition position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double reserve(RailLink link, TrainPosition position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void release(RailLink link, MobsimDriverAgent driver) {
		throw new UnsupportedOperationException();
	}
}
