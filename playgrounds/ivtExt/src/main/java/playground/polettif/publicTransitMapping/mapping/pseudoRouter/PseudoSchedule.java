/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.mapping.pseudoRouter;

import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.List;
import java.util.Set;

/**
 * Container to store multiple {@link PseudoTransitRoute}. Provides a
 * method to modify an input schedule by creating child StopFacilities.
 * <p></p>
 *
 * <ul><li>PseudoSchedule</li>
 *     <ul><li>{@link PseudoTransitRoute}</li>
 *         <ul><li>List of {@link PseudoRouteStop}</li>
 *         </ul>
 *     </ul>
 * </ul>
 * <p></p>
 *
 * {@link PseudoGraph} uses PseudoRouteStops as nodes and is used to calculate
 * the best sequence of PseudoRouteStops for a route.
 *
 * @author polettif
 */
public interface PseudoSchedule {

	void addPseudoRoute(TransitLine transitLine, TransitRoute transitRoute, List<PseudoRouteStop> pseudoStopSequence);

	Set<PseudoTransitRoute> getPseudoRoutes();

	/**
	 * Merges the other pseudo schedule into this pseudo schedule
	 */
	void mergePseudoSchedule(PseudoSchedule otherPseudoSchedule);

	/**
	 * Replaces the stop facilities in the given schedule based on
	 * the PseudoRoutes of the PseudoSchedule. Every parent stop
	 * facility in the schedule's routeProfiles is replaced with a
	 * child stop facility.
	 *
	 * @param schedule where stop facilities should be replaced
	 */
	void createAndReplaceFacilities(TransitSchedule schedule);

}

