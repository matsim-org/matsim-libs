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

package playground.julia.newSpatialAveraging;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

public class EmissionsPerLinkAndTimeIntervalEventHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler{

	public EmissionsPerLinkAndTimeIntervalEventHandler(
			double simulationEndTime, int noOfTimeBins, String pollutant2analyze) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		// TODO Auto-generated method stub
		
	}

	public Map<Integer, Map<Id, Double>> getTimeIntervals2EmissionsPerLink() {
		// TODO Auto-generated method stub
		return null;
	}

}
