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

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 
 * @author aneumann
 *
 */
class PTransitRouterFactory implements TransitRouterFactory{
	
	private final static Logger log = Logger.getLogger(PTransitRouterFactory.class);
	private TransitRouterConfig transitRouterConfig;
	private final String ptEnabler;
	private boolean needToUpdateRouter = true;
	private TransitRouterNetwork routerNetwork = null;
	private TransitRouterFactory routerFactory = null;
	private TransitSchedule schedule;
	
	public PTransitRouterFactory(String ptEnabler){
		this.ptEnabler = ptEnabler;
	}

	public void createTransitRouterConfig(Config config) {
		this.transitRouterConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
	}
	
	public void updateTransitSchedule(TransitSchedule schedule) {
		this.needToUpdateRouter = true; 
		this.schedule = schedule;
	}

	@Override
	public TransitRouter createTransitRouter() {
		if(needToUpdateRouter) {
			// okay update all routers
			this.routerFactory = createSpeedyRouter();
			if(this.routerFactory == null) {
				log.warn("Could not create speedy router, fall back to normal one.");
				this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.transitRouterConfig.beelineWalkConnectionDistance);
			}
			needToUpdateRouter = false;
		}
		
		if (this.routerFactory == null) {
			// no speedy router available - return old one
			PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(schedule);
			TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.transitRouterConfig, preparedTransitSchedule);
			return new TransitRouterImpl(this.transitRouterConfig, preparedTransitSchedule, routerNetwork, ttCalculator, ttCalculator);
		} else {
			return this.routerFactory.createTransitRouter();
		}
	}
	
	private TransitRouterFactory createSpeedyRouter() {
		try {
			Class<?> cls = Class.forName("com.senozon.matsim.pt.speedyrouter.SpeedyTransitRouterFactory");
			Constructor<?> ct = cls.getConstructor(new Class[] {TransitSchedule.class, TransitRouterConfig.class, String.class});
			return (TransitRouterFactory) ct.newInstance(this.schedule, this.transitRouterConfig, this.ptEnabler);
		} catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
        return null;
	}
}
