/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.pt;

import org.matsim.contrib.minibus.performance.raptor.Raptor;
import org.matsim.contrib.minibus.performance.raptor.RaptorDisutility;
import org.matsim.contrib.minibus.performance.raptor.TransitRouterQuadTree;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterImplTest;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.agarwalamit.pt.connectionScan.ConnectionScanRouter;

/**
 * Created by amit on 16.05.17.
 */


public class TransitRouterImplTestFromCore extends TransitRouterImplTest {

    private static TransitRouter createTransitRouter(TransitSchedule schedule, TransitRouterConfig trConfig, TransitRouterType routerType) {
        TransitRouter router = null ;
        switch( routerType ) {
            case raptor:
                double costPerMeterTraveled = 0.;
                double costPerBoarding = 0.;
                RaptorDisutility raptorDisutility = new RaptorDisutility(trConfig, costPerBoarding, costPerMeterTraveled);
                TransitRouterQuadTree transitRouterQuadTree = new TransitRouterQuadTree(raptorDisutility);
                transitRouterQuadTree.initializeFromSchedule(schedule, trConfig.getBeelineWalkConnectionDistance());
                router = new Raptor(transitRouterQuadTree, raptorDisutility, trConfig) ;
                break;
            case standard:
                router = new TransitRouterImpl(trConfig, schedule);
                break;
            case connectionScan:
                router = new ConnectionScanRouter();
                break;
            default:
                break;
        }
        return router;
    }

}