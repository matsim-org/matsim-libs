package org.matsim.core.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

public class FacilityWrapperActivity implements Activity {
	private final Facility wrapped;

	public FacilityWrapperActivity(final Facility toWrap) {
		this.wrapped = toWrap;
	}

	@Override
	public double getEndTime() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setEndTime(double seconds) {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public String getType() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setType(String type) {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public Coord getCoord() {
		return wrapped.getCoord();
	}

	@Override
	public double getStartTime() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setStartTime(double seconds) {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public double getMaximumDuration() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setMaximumDuration(double seconds) {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public Id<Link> getLinkId() {
		return wrapped.getLinkId();
	}

	@Override
	public Id<ActivityFacility> getFacilityId() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public String toString() {
		return "[FacilityWrapper: wrapped="+wrapped+"]";
	}

	@Override
	public void setLinkId(Id<Link> id) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setFacilityId(Id<ActivityFacility> id) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
}