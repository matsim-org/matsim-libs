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

package playground.andreas.P2.schedule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * 
 * @author aneumann
 *
 */
public class PTransitRouterFactory implements TransitRouterFactory{
	
	private final static Logger log = Logger.getLogger(PTransitRouterFactory.class);
	private TransitRouterConfig transitRouterConfig;
	private String ptEnabler;
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
			TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.transitRouterConfig);
			return new TransitRouterImpl(this.transitRouterConfig, routerNetwork, ttCalculator, ttCalculator);
		} else {
			return this.routerFactory.createTransitRouter();
		}
	}
	
	private TransitRouterFactory createSpeedyRouter() {
		try {
			Class<?> cls = Class.forName("com.senozon.matsim.pt.speedyrouter.SpeedyTransitRouterFactory");
			Constructor<?> ct = cls.getConstructor(new Class[] {TransitSchedule.class, TransitRouterConfig.class, String.class});
			return (TransitRouterFactory) ct.newInstance(new Object[] {this.schedule, this.transitRouterConfig, this.ptEnabler});
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}
