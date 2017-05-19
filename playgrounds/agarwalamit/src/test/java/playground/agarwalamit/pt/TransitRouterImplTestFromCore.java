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

import java.util.Arrays;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.junit.runners.Parameterized.Parameters;
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
 * The idea is to keep the core test in place and still able to test other two transit router types.
 *
 * Created by amit on 16.05.17.
 */


public class TransitRouterImplTestFromCore extends TransitRouterImplTest {
	private static final Logger log = Logger.getLogger(TransitRouterImplTestFromCore.class) ;
	
	private String routerType ;
	// yyyyyy probably better make type a String ... no point to have an enum in the core that needs to be touched every time a new router is
	// pulled underneath this test.

	@Parameters(name = "{index}: TransitRouter == {0}")
	public static Collection<Object> createRouterTypes() {
		Object[] router = new Object [] { 
				"standard",
//                "raptor",
//                "connectionScan"
		};
		return Arrays.asList(router);
	}

	public TransitRouterImplTestFromCore( String routerType ) {
		super( routerType ) ;
		log.warn( "using router=" + routerType ) ;
		this.routerType = routerType;
	}


    protected TransitRouter createTransitRouter(TransitSchedule schedule, TransitRouterConfig trConfig, String routerType) {
        TransitRouter router = null ;
        switch( routerType ) {
            case "standard":
                router = new TransitRouterImpl(trConfig, schedule);
                break;
            case "raptor":
                double costPerMeterTraveled = 0.;
                double costPerBoarding = 0.;
                RaptorDisutility raptorDisutility = new RaptorDisutility(trConfig, costPerBoarding, costPerMeterTraveled);
                TransitRouterQuadTree transitRouterQuadTree = new TransitRouterQuadTree(raptorDisutility);
                transitRouterQuadTree.initializeFromSchedule(schedule, trConfig.getBeelineWalkConnectionDistance());
                router = new Raptor(transitRouterQuadTree, raptorDisutility, trConfig) ;
                break;
            case "connectionScan":
                router = new ConnectionScanRouter();
                break;
            default:
                break;
        }
        return router;
    }

}