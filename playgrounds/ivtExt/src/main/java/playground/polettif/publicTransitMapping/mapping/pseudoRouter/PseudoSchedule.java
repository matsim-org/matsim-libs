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
 * todo javadoc
 */
public interface PseudoSchedule {

	void addPseudoRoute(TransitLine transitLine, TransitRoute transitRoute, List<PseudoRouteStop> pseudoStopSequence);

	Set<PseudoRoute> getPseudoRoutes();

	void mergePseudoSchedule(PseudoSchedule otherPseudoSchedule);

	void createAndReplaceFacilities(TransitSchedule schedule);

}

