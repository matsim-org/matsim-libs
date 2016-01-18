/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.michalm.taxi.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.router.util.*;

import playground.michalm.taxi.scheduler.*;


public class TaxiOptimizers
{
    public static TaxiOptimizer createOptimizer(MatsimVrpContext context, TravelTime travelTime,
            AbstractTaxiOptimizerParams optimizerParams, TaxiSchedulerParams schedulerParams)
    {
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
        TaxiScheduler scheduler = new TaxiScheduler(context, schedulerParams, travelTime,
                travelDisutility);
        TaxiOptimizerContext optimContext = new TaxiOptimizerContext(context, travelTime,
                travelDisutility, optimizerParams, scheduler);
        return optimizerParams.createTaxiOptimizer(optimContext);
    }
}
