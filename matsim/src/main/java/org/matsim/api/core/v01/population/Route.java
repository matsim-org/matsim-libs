/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.api.core.v01.population;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimPopulationObject;

/**
 * @author nagel
 *
 */
public interface Route extends MatsimPopulationObject {

	/**Design thoughts:<ul>
	 * <li>I am not convinced that this should really be removed.  There is no harm in having an "expected distance".
	 * The main problem, presumably, is that people confuse "planned" distance and "executed" distance.  The latter needs
	 * to come from somewhere else (e.g. from RouteUtils.calcDistance()).  kai, jun'11
	 * <li>I would, in fact, prefer to have this in leg.  (Because then I could do teleportation based on legs.) 
	 * But on the other hand, I could also live with having it in the route, since it is already there. kai, jun'11  
	 * </ul> 
	 * @deprecated  use RouteUtils.calcDistance()  */
	@Deprecated
	public double getDistance();

	// once getDistance is removed in the code, remove this as well
	public void setDistance(final double distance);

	/** Design thoughts:<ul>
	 * <li>This used to be "deprecated" since I would have preferred to have this in the Leg.  But since the distance is already
	 * in the Route (which I would like to keep, see above), it maybe makes sense to also leave this here in the Route. 
	 * kai, jun'11
	 * </ul>
	 */
	public double getTravelTime();

	/** Design thoughts:<ul>
	 * <li>This used to be "deprecated" since I would have preferred to have this in the Leg.  But since the distance is already
	 * in the Route (which I would like to keep, see above), it maybe makes sense to also leave this here in the Route. 
	 * kai, jun'11
	 * </ul>
	 */
	public void setTravelTime(final double travelTime);

	public Id getStartLinkId();

	public Id getEndLinkId();

}
