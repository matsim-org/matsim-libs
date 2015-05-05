/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.wagonSim.pt.router.deprecated;

import org.apache.log4j.Logger;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.ObjectAttributes;

import javax.inject.Provider;

/**
 * 
 * This is a much more custom-version, using {@link WagonSimRouterFactory2Impl}.
 * Currently it is not working!
 * 
 * @author droeder
 *
 */
@Deprecated
public class WagonSimRouterFactory2Impl implements Provider<TransitRouter> {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(WagonSimRouterFactory2Impl.class);
	private WagonSimVehicleLoadListener vehLoad;
	private TransitRouterConfig config;
	private TransitSchedule schedule;
	private TransitRouterNetwork routerNetwork;
	private PreparedTransitSchedule preparedTransitSchedule;
	private ObjectAttributes locomotiveAttribs;
	private ObjectAttributes wagonAttribs;

	public WagonSimRouterFactory2Impl(WagonSimVehicleLoadListener vehLoad, 
				TransitSchedule schedule, 
				TransitRouterConfig config,
				ObjectAttributes wagonAttribs,
				ObjectAttributes locomotiveAttribs) {
		this.vehLoad = vehLoad;
		this.schedule = schedule;
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
		this.locomotiveAttribs = locomotiveAttribs;
		this.wagonAttribs = wagonAttribs;
	}

	@Override
	public TransitRouter get() {
		WagonSimRouterNetworkTravelDistutilityAndTravelTime2 tt = 
				new WagonSimRouterNetworkTravelDistutilityAndTravelTime2(
						config, 
						preparedTransitSchedule, 
						vehLoad.getLoadOfLastIter(),
						this.locomotiveAttribs,
						this.wagonAttribs);
		return new TransitRouterImpl(config, preparedTransitSchedule, routerNetwork, tt, tt);
	}
	
}

