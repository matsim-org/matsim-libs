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

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;

/**
 * @author johannes
 *
 */
public class ProxyLegAdaptor implements Leg {

	private final Element delegate;
	
	public ProxyLegAdaptor(Element leg) {
		this.delegate = leg;
	}
	
	@Override
	public String getMode() {
		return delegate.getAttribute(CommonKeys.LEG_MODE);
	}

	@Override
	public void setMode(String mode) {
		throw new UnsupportedOperationException("This is read only data container.");
	}

	@Override
	public Route getRoute() {
		return null;
	}

	@Override
	public void setRoute(Route route) {
		throw new UnsupportedOperationException("This is read only data container.");
	}

	@Override
	public double getDepartureTime() {
		String val = delegate.getAttribute(CommonKeys.LEG_START_TIME);
		if(val == null) {
			return Double.NaN;
		}
		
		return Double.parseDouble(val);
	}

	@Override
	public void setDepartureTime(double seconds) {
		throw new UnsupportedOperationException("This is read only data container.");
	}

	@Override
	public double getTravelTime() {
		String start = delegate.getAttribute(CommonKeys.LEG_START_TIME);
		String end = delegate.getAttribute(CommonKeys.LEG_END_TIME);
		if(start != null && end != null) {
			double s = Double.parseDouble(start);
			double e = Double.parseDouble(end);
			return e - s;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public void setTravelTime(double seconds) {
		throw new UnsupportedOperationException("This is read only data container.");
	}

}
