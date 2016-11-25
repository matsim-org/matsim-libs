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

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.performance.raptor.Raptor;
import org.matsim.contrib.minibus.performance.raptor.RaptorDisutility;
import org.matsim.contrib.minibus.performance.raptor.TransitRouterQuadTree;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Provider;

/**
 * 
 * @author aneumann
 *
 */
class PTransitRouterFactory implements Provider<TransitRouter> {
	
	private final static Logger log = Logger.getLogger(PTransitRouterFactory.class);
	private TransitRouterConfig transitRouterConfig;
	private final String ptEnabler;
	private final String ptRouter;
	private final double costPerBoarding;
	private final double costPerMeterTraveled;
	
	private boolean needToUpdateRouter = true;
	private TransitRouterNetwork routerNetwork = null;
	private Provider<TransitRouter> routerFactory = null;
	private TransitSchedule schedule;
	private RaptorDisutility raptorDisutility;
	private TransitRouterQuadTree transitRouterQuadTree;
	
	public PTransitRouterFactory(String ptEnabler, String ptRouter, double costPerBoarding, double costPerMeterTraveled){
		this.ptEnabler = ptEnabler;
		this.ptRouter = ptRouter;
		this.costPerBoarding = costPerBoarding;
		this.costPerMeterTraveled = costPerMeterTraveled;
	}

	public void createTransitRouterConfig(Config config) {
		this.transitRouterConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
	}
	
	public void updateTransitSchedule(TransitSchedule schedule) {
		this.needToUpdateRouter = true;
		this.schedule = schedule;
//		this.schedule = PTransitLineMerger.mergeSimilarRoutes(schedule);
		
		if (this.ptRouter.equalsIgnoreCase("raptor")) {
			// this could also hold updated prices
			this.raptorDisutility = new RaptorDisutility(this.transitRouterConfig, this.costPerBoarding, this.costPerMeterTraveled);
			
			this.transitRouterQuadTree = new TransitRouterQuadTree(this.raptorDisutility);
			this.transitRouterQuadTree.initializeFromSchedule(this.schedule, this.transitRouterConfig.getBeelineWalkConnectionDistance());
		}
	}

	@Override
	public TransitRouter get() {
		if(needToUpdateRouter) {
			// okay update all routers
			this.routerFactory = createSpeedyRouter();
			if(this.routerFactory == null) {
				if (this.ptRouter.equalsIgnoreCase("raptor")) {
					// nothing to do here
				} else {
					log.warn("Could not create speedy router, fall back to normal one.  This is so far not fatal.");
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
		return new Raptor(this.transitRouterQuadTree, this.raptorDisutility, this.transitRouterConfig);
	}

	private Provider<TransitRouter> createSpeedyRouter() {
		try {
			Class<?> cls = Class.forName("com.senozon.matsim.pt.speedyrouter.SpeedyTransitRouterFactory");
			Constructor<?> ct = cls.getConstructor(new Class[] {TransitSchedule.class, TransitRouterConfig.class, String.class});
			return (Provider<TransitRouter>) ct.newInstance(this.schedule, this.transitRouterConfig, this.ptEnabler);
		} catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
        return null;
	}
}
