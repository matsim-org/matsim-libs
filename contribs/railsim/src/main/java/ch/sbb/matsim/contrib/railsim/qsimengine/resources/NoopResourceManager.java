package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.Collection;
import java.util.List;

/**
 * Resource manager that always approves requests.
 */
public final class NoopResourceManager implements RailResourceManager {

	private final RailResourceManager delegate;

	public NoopResourceManager(RailResourceManager delegate) {
		this.delegate = delegate;
	}

	@Override
	public Collection<RailResource> getResources() {
		return delegate.getResources();
	}

	@Override
	public RailLink getLink(Id<Link> id) {
		return delegate.getLink(id);
	}

	@Override
	public double tryBlockLink(double time, RailLink link, int track, TrainPosition position) {
		return link.getLength();
	}

	@Override
	public boolean hasCapacity(double time, Id<Link> link, int track, TrainPosition position) {
		return true;
	}

	@Override
	public void setCapacity(Id<Link> link, int newCapacity) {

	}

	@Override
	public boolean isBlockedBy(RailLink link, TrainPosition position) {
		return true;
	}

	@Override
	public void releaseLink(double time, RailLink link, MobsimDriverAgent driver) {
	}

	@Override
	public boolean checkReroute(double time, RailLink start, RailLink end, List<RailLink> subRoute, List<RailLink> detour, TrainPosition position) {
		return false;
	}
}
