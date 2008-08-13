/* *********************************************************************** *
 * project: org.matsim.*
 * EmptyControlInputImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmanagement;

import org.matsim.population.Route;
import org.matsim.withinday.trafficmanagement.controlinput.AbstractControlInputImpl;



/**
 * @author dgrether
 *
 */
public class EmptyControlInputImpl extends AbstractControlInputImpl {

	private double nashTime;

	public EmptyControlInputImpl() {
	}

	/**
	 * @see org.matsim.withinday.trafficmanagement.ControlInput#getNashTime()
	 */
	@Override
	public double getNashTime() {
		return this.nashTime;
	}

	/**
	 * @see org.matsim.events.handler.EventHandler#reset(int)
	 */
	public void reset(final int iteration) {
	}

	public void setNashTime(final double t) {
		this.nashTime = t;
	}

	@Override
	public void finishIteration() {
	}

	@Override
	public double getPredictedNashTime(Route route) {
		// TODO Auto-generated method stub
		return 0;
	}

}
