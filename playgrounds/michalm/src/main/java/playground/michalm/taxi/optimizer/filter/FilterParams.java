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

package playground.michalm.taxi.optimizer.filter;

public class FilterParams
{
    public final Integer nearestRequestsLimit;//null ==> no filtration
    public final Integer nearestVehiclesLimit;//null ==> no filtration


    public FilterParams(Integer nearestRequestsLimit, Integer nearestVehiclesLimit)
    {
        this.nearestRequestsLimit = nearestRequestsLimit;
        this.nearestVehiclesLimit = nearestVehiclesLimit;
    }
}
