/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.optimizer;

import java.util.*;

import org.matsim.contrib.dvrp.data.Vehicle;

import playground.jbischoff.taxibus.optimizer.filter.*;
import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.passenger.TaxibusRequest.TaxibusRequestStatus;
import playground.jbischoff.taxibus.vehreqpath.*;


/**
 * @author jbischoff
 */
public class DefaultTaxibusOptimizer
    extends AbstractTaxibusOptimizer
{

    private Set<Vehicle> idleVehicles;
    private final TaxibusVehicleFilter vehicleFilter;
    private final TaxibusRequestFilter requestFilter;
    private final TaxibusVehicleRequestPathFinder vrpFinder;


    public DefaultTaxibusOptimizer(TaxibusOptimizerConfiguration optimConfig,
            boolean doUnscheduleAwaitingRequests)
    {
        super(optimConfig, doUnscheduleAwaitingRequests);
        this.vehicleFilter = optimConfig.filterFactory.createVehicleFilter();
        this.requestFilter = optimConfig.filterFactory.createRequestFilter();
        vrpFinder = new TaxibusVehicleRequestPathFinder(optimConfig);
    }


    @Override
    protected void scheduleUnplannedRequests()
    {

        initPossibleVehicles();
        scheduleUnplannedRequestsImpl();
    }


    private void initPossibleVehicles()
    {
        idleVehicles = new LinkedHashSet<>();
        for (Vehicle veh : this.optimConfig.context.getVrpData().getVehicles().values()) {
            System.out.println(veh.getSchedule().getStatus());
            if (optimConfig.scheduler.isIdle(veh)) {
                idleVehicles.add(veh);
            }
        }
    }


    private void scheduleUnplannedRequestsImpl()
    {
        System.out.println(unplannedRequests);
        Iterator<TaxibusRequest> reqIter = unplannedRequests.iterator();

        while (reqIter.hasNext() && !idleVehicles.isEmpty()) {
            TaxibusRequest req = reqIter.next();
            TaxibusRequestStatus cs = req.getStatus();
            if (req.getStatus() != TaxibusRequestStatus.UNPLANNED) {
                reqIter.remove();
                continue;
            }
            Iterable<Vehicle> filteredVehs = vehicleFilter.filterVehiclesForRequest(idleVehicles,
                    req);
            TaxibusVehicleRequestPath best = vrpFinder.findBestVehicleForRequest(req, filteredVehs,
                    TaxibusVehicleRequestPaths.TW_COST);

            if (best != null) {
                reqIter.remove();
                boolean possibleOtherRequest = true;
                Iterable<TaxibusRequest> filteredReqs = requestFilter
                        .filterRequestsForBestRequest(unplannedRequests, best);
                do {

                    TaxibusVehicleRequestPath nextBest = vrpFinder
                            .findBestAdditionalVehicleForRequestPath(best, filteredReqs);
                    if (nextBest != null) {
                        best = nextBest;

                    }
                    else {
                        possibleOtherRequest = false;
                    }
                }
                while (possibleOtherRequest);
                //	              	TaxibusUtils.printRequestPath(best);

                optimConfig.scheduler.scheduleRequest(best);

            }

        }

    }

}
