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

package contrib.publicTransitMapping.mapping.pseudoRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.List;

/**
 * A container class for a {@link PseudoRouteStop} sequence. It is
 * used to store the original transit route and transit line to
 * recreate a TransitSchedule from PseudoTransitRoutes.
 *
 * @author polettif
 */
public interface PseudoTransitRoute {

	Id<TransitLine> getTransitLineId();

	TransitRoute getTransitRoute();

	/**
	 * @return The Sequence of PseudoRouteStops i.e. the sequence of
	 * link candidates that are used by this PseudoTransitRoute
	 */
	List<PseudoRouteStop> getPseudoStops();
}
