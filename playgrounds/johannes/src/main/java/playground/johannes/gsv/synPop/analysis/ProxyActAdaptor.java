/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.gsv.synPop.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;

/**
 * @author johannes
 *
 */
public class ProxyActAdaptor implements Activity {

	private final Attributable delegate;
	
	public ProxyActAdaptor(Attributable act) {
		this.delegate = act;
	}
	
	@Override
	public double getEndTime() {
		String val = delegate.getAttribute(CommonKeys.ACTIVITY_END_TIME);
		if(val == null) {
			return Double.NaN;
		} else {
			return Double.parseDouble(val);
		}
	}

	@Override
	public void setEndTime(double seconds) {
		throw new UnsupportedOperationException("This is read only data container.");
	}

	@Override
	public String getType() {
		return delegate.getAttribute(CommonKeys.ACTIVITY_TYPE);
	}

	@Override
	public void setType(String type) {
		throw new UnsupportedOperationException("This is read only data container.");
	}

	@Override
	public Coord getCoord() {
		// TODO Currently not implemented, yet is possible.
		return null;
	}

	@Override
	public double getStartTime() {
		String val = delegate.getAttribute(CommonKeys.ACTIVITY_START_TIME);
		if(val == null) {
			return Double.NaN;
		} else {
			return Double.parseDouble(val);
		}
	}

	@Override
	public void setStartTime(double seconds) {
		throw new UnsupportedOperationException("This is read only data container.");
	}

	@Override
	public double getMaximumDuration() {
		return Double.NaN;
	}

	@Override
	public void setMaximumDuration(double seconds) {
		throw new UnsupportedOperationException("This is read only data container.");
	}

	@Override
	public Id<Link> getLinkId() {
		// TODO Currently not implemented, yet is possible.
		return null;
	}

	@Override
	public Id<ActivityFacility> getFacilityId() {
		return Id.create(delegate.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
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
