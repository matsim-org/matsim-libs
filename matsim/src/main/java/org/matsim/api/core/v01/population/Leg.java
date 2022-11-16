/* *********************************************************************** *
 * project: org.matsim.*
 * Leg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import org.matsim.core.utils.misc.OptionalTime;

public interface Leg extends PlanElement {

    public String getMode();

    /**
     * Sets the mode of the leg. No consistency check is done; in particular, the route or travel time info from
     * a different mode may remain in place.
     */
    public void setMode(String mode);

    public String getRoutingMode();

    public void setRoutingMode(String routingMode);

    public Route getRoute();

    public void setRoute(Route route);

    public OptionalTime getDepartureTime();

    public void setDepartureTime(final double seconds);

    public void setDepartureTimeUndefined();

    /**
     * Design thoughts:<ul>
     * <li>There is also a getTravelTime in the route.  One of these should go.  Given that there is also an
     * getDistance in the Route, but not in the leg, it is maybe more pragmatic to remove it from the leg?
     * kai/benjamin, jun'11
     * </ul>
     */
    public OptionalTime getTravelTime();

    public void setTravelTime(final double seconds);

    public void setTravelTimeUndefined();
}