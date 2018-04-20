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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
	
	private final static Logger log = Logger.getLogger(PTransitRouterFactory.class);
	private TransitRouterConfig transitRouterConfig;
	private final String ptEnabler;
	private final String ptRouter;
	private final double costPerBoarding;
	private final double costPerMeterTraveled;
	
	private boolean needToUpdateRouter = true;
	private TransitRouterNetwork routerNetwork = null;
	private Provider<TransitRouter> routerFactory = null;
	@Inject private TransitSchedule schedule;
	private RaptorDisutility raptorDisutility;
	
	// SwissRailRaptor
    private SwissRailRaptorData swissRailRaptorData = null;
    private final RaptorConfig swissRailRaptorConfig;
    private final RaptorRouteSelector swissRailRaptorRouteSelector = new LeastCostRaptorRouteSelector();

	public PTransitRouterFactory(Config config){
		PConfigGroup pConfig = ConfigUtils.addOrGetModule(config, PConfigGroup.class) ;
		this.ptEnabler = pConfig.getPtEnabler() ;
		this.ptRouter = pConfig.getPtRouter() ;
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
		
		if (this.ptRouter.equalsIgnoreCase("raptor")) {
			// this could also hold updated prices
			this.raptorDisutility = new RaptorDisutility(this.transitRouterConfig, this.costPerBoarding, this.costPerMeterTraveled);
		} else if (this.ptRouter.equalsIgnoreCase("SwissRailRaptor")){
			swissRailRaptorData = null;
		}
	}

	@Override
	public TransitRouter get() {
		if(needToUpdateRouter) {
			// okay update all routers
			this.routerFactory = createSpeedyRouter();
			if(this.routerFactory == null) {
				if (this.ptRouter.equalsIgnoreCase("raptor") || this.ptRouter.equalsIgnoreCase("SwissRailRaptor")) {
					// nothing to do here
				} else {
					log.info("Could not create speedy router, fall back to normal one.  This is so far not fatal.");
//					Gbl.assertNotNull(this.schedule);
					Gbl.assertNotNull(this.transitRouterConfig);
					this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.transitRouterConfig.getBeelineWalkConnectionDistance());
				}
			}
			needToUpdateRouter = false;
		}
		
		if (this.routerFactory == null) {
			if (this.ptRouter.equalsIgnoreCase("raptor")) {
				log.info("Using raptor routing");
				return this.createRaptorRouter();
			} else if (this.ptRouter.equalsIgnoreCase("SwissRailRaptor")) {
				log.info("Using SwissRailRaptor routing");
				return this.createSwissRailRaptor();
			} else {
				// no speedy router available - return old one
				PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(schedule);
				TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.transitRouterConfig, preparedTransitSchedule);
				return new TransitRouterImpl(this.transitRouterConfig, preparedTransitSchedule, routerNetwork, ttCalculator, ttCalculator);
			}
		} else {
			return this.routerFactory.get();
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
	
	private Provider<TransitRouter> createSpeedyRouter() {
		try {
			Class<?> cls = Class.forName("com.senozon.matsim.pt.speedyrouter.SpeedyTransitRouterFactory");
			Constructor<?> ct = cls.getConstructor(new Class[] {TransitSchedule.class, TransitRouterConfig.class, String.class});
			return (Provider<TransitRouter>) ct.newInstance(this.schedule, this.transitRouterConfig, this.ptEnabler);
		} catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
//			e.printStackTrace();
			// I don't like output to stderr when the program execution is actually ok.  kai, jan'16
			log.info(e.toString() );
		}
        return null;
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
