/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.analysis;

import java.util.List;
import java.util.Vector;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;


public class TTCalculator implements BasicEventHandler {	
	private List<Double> travelTimes = new Vector<Double>();
	
	@Override
	public void handleEvent(final Event event) {
		double tt = 1.0;
		this.travelTimes.add(tt);
	}

	@Override
	public void reset(final int iteration) {
		this.travelTimes.clear();
	}
}
