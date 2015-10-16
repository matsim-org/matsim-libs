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

package playground.jbischoff.taxibus.optimizer.filter;

import playground.jbischoff.taxibus.scheduler.TaxibusScheduler;


public class DefaultTaxibusFilterFactory
    implements TaxibusFilterFactory
{
    private final TaxibusScheduler scheduler;
    private final int nearestVehiclesLimit;
    private final int nearestRequestsLimit;

    public DefaultTaxibusFilterFactory(TaxibusScheduler scheduler, int nearestRequestsLimit,
            int nearestVehiclesLimit)
    {
        this.scheduler = scheduler;
        this.nearestRequestsLimit = nearestRequestsLimit;
        this.nearestVehiclesLimit = nearestVehiclesLimit;
    }


    @Override
    public TaxibusVehicleFilter createVehicleFilter()
    {
        return nearestVehiclesLimit <= 0 ? TaxibusVehicleFilter.NO_FILTER
                : new KStraightLineNearestVehicleFilter(scheduler, nearestVehiclesLimit);
    }
    @Override
    public TaxibusRequestFilter createRequestFilter()
    {
        return nearestRequestsLimit <= 0 ? TaxibusRequestFilter.NO_FILTER
                : new KStraightLineNearestRequestFilter(scheduler, nearestRequestsLimit);
    }

   
}
