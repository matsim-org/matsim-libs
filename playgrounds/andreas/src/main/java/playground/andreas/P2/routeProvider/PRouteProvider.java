/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.andreas.P2.routeProvider;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.replanning.PPlan;

public interface PRouteProvider {
	
	public TransitLine createTransitLine(Id lineId, PPlan plan);

	public TransitStopFacility getRandomTransitStop(int currentIteration);
	
	public TransitStopFacility drawRandomStopFromList(List<TransitStopFacility> choiceSet);
	
	public Collection<TransitStopFacility> getAllPStops();

	public TransitLine createEmptyLine(Id id);

}
