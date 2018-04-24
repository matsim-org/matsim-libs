/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.hook;

import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.performance.raptor.Raptor;
import org.matsim.contrib.minibus.performance.raptor.RaptorDisutility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

/**
 * 
 * @author aneumann
 *
 */
class PTransitRouterFactory implements Provider<TransitRouter> {
	// How is this working if nothing is injected?  But presumably it uses "Provider" only as a syntax clarifier, but the class
	// is not injectable. kai, jun'16
	
    private static enum RouterType {
    	STANDARD, ANRAPTOR, SBBRAPTOR;
    }
	
	private final static Logger log = Logger.getLogger(PTransitRouterFactory.class);
	private TransitRouterConfig transitRouterConfig;
	private final RouterType ptRouter;
	private final double costPerBoarding;
	private final double costPerMeterTraveled;
	
	private boolean needToUpdateRouter = true;
	private TransitRouterNetwork routerNetwork = null;
	@Inject private TransitSchedule schedule;
	private RaptorDisutility raptorDisutility;
	
	// SwissRailRaptor
    private SwissRailRaptorData swissRailRaptorData = null;
    private final RaptorConfig swissRailRaptorConfig;
    private final RaptorRouteSelector swissRailRaptorRouteSelector = new LeastCostRaptorRouteSelector();
    
	public PTransitRouterFactory(Config config){
		PConfigGroup pConfig = ConfigUtils.addOrGetModule(config, PConfigGroup.class) ;
		switch (pConfig.getPtRouter()) {
		case "standard": 
			ptRouter = RouterType.STANDARD; 
			break;
		case "raptor": 
			ptRouter = RouterType.ANRAPTOR; 
			break;
		case "SwissRailRaptor": 
			ptRouter = RouterType.SBBRAPTOR;
			break;
		default: 
			ptRouter = RouterType.STANDARD;
			break;
		}
		
		this.costPerBoarding = pConfig.getEarningsPerBoardingPassenger() ;
		this.costPerMeterTraveled = pConfig.getEarningsPerKilometerAndPassenger() ;
		
		this.createTransitRouterConfig(config);
		this.swissRailRaptorConfig = RaptorUtils.createRaptorConfig(config);
	}

	private void createTransitRouterConfig(Config config) {
		this.transitRouterConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
	}
	
	void updateTransitSchedule() {
		this.needToUpdateRouter = true;
//		this.schedule = PTransitLineMerger.mergeSimilarRoutes(schedule);
		switch (ptRouter) {
		case ANRAPTOR: 
			this.raptorDisutility = new RaptorDisutility(this.transitRouterConfig, this.costPerBoarding, this.costPerMeterTraveled);
			break;
		case SBBRAPTOR:
			swissRailRaptorData = null;
			break;
		default:
			// no action needed for the other routers
			break;
		}
	}

	@Override
	public TransitRouter get() {
		if (needToUpdateRouter) {
			// okay update all routers
			switch (ptRouter) {
			case STANDARD:
//				Gbl.assertNotNull(this.schedule);
				Gbl.assertNotNull(this.transitRouterConfig);
				this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, 
						this.transitRouterConfig.getBeelineWalkConnectionDistance());
				break;
			default:
				// no action needed for other routers
				break;
			}
			needToUpdateRouter = false;
		}
		
		switch (ptRouter) {
		case STANDARD:
			PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(schedule);
			TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.transitRouterConfig, preparedTransitSchedule);
			return new TransitRouterImpl(this.transitRouterConfig, preparedTransitSchedule, routerNetwork, ttCalculator, ttCalculator);
		case ANRAPTOR:
			log.info("Using raptor routing");
			return this.createRaptorRouter();
		case SBBRAPTOR:
			log.info("Using SwissRailRaptor routing");
			return this.createSwissRailRaptor();
		default: 
			// make the compiler happy even though the constructor excludes this case
			log.error("Unknown router type.");
			return null;
		}
	}
	
	private TransitRouter createRaptorRouter() {
		if ( this.raptorDisutility == null) {
			updateTransitSchedule();
		}
        return new Raptor(this.transitRouterConfig, this.schedule, this.raptorDisutility);
	}
	
	private TransitRouter createSwissRailRaptor() {
        SwissRailRaptorData data = getSwissRailRaptorData();
		return new SwissRailRaptor(data, this.swissRailRaptorRouteSelector);
	}

    private SwissRailRaptorData getSwissRailRaptorData() {
        if (this.swissRailRaptorData == null) {
            this.swissRailRaptorData = prepareSwissRailRaptorData();
        }
        return this.swissRailRaptorData;
    }

    synchronized private SwissRailRaptorData prepareSwissRailRaptorData() {
        if (this.swissRailRaptorData != null) {
            // due to multithreading / race conditions, this could still happen.
            // prevent doing the work twice.
            return this.swissRailRaptorData;
        }
        log.info("Updating SwissRailRaptor data to new minibus schedule");
        this.swissRailRaptorData = SwissRailRaptorData.create(this.schedule, this.swissRailRaptorConfig);
        return this.swissRailRaptorData;
    }
	
	//see MATSim-768
//	void notifyIterationStarts(IterationStartsEvent event) {
//		this.updateTransitSchedule();
//	}
//
//	void notifyStartup(StartupEvent event) {
//		this.updateTransitSchedule();
//	}
}
