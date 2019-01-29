package org.matsim.core.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import java.util.Map;

/*
 * Wraps a Link into a Facility.
 */
public final class LinkWrapperFacility implements Facility, Identifiable<ActivityFacility> {
	
	private final Link wrapped;

	public LinkWrapperFacility(final Link toWrap) {
		wrapped = toWrap;
	}

	@Override
	public Coord getCoord() {
		return wrapped.getCoord();
	}

	@Override
	public Id<ActivityFacility> getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id<Link> getLinkId() {
		return wrapped.getId();
	}

	@Override
	public String toString() {
		return "[LinkWrapperFacility: wrapped="+wrapped+"]";
	}
}
