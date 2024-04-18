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

package org.matsim.contrib.decongestion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;
import org.matsim.contrib.decongestion.handler.IntervalBasedTolling;
import org.matsim.contrib.decongestion.handler.IntervalBasedTollingAll;
import org.matsim.contrib.decongestion.handler.PersonVehicleTracker;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollSetting;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollingBangBang;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollingPID;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollingP_MCP;
import org.matsim.core.controler.AbstractModule;

/**
* @author ikaddoura
*/

public class DecongestionModule extends AbstractModule {

	@Inject private DecongestionConfigGroup decongestionConfigGroup;

	@Override
	public void install() {
		
		if (decongestionConfigGroup.isEnableDecongestionPricing()) {
			switch( decongestionConfigGroup.getDecongestionApproach() ){
				case BangBang -> {
					this.bind( DecongestionTollingBangBang.class ).in( Singleton.class );
					this.bind( DecongestionTollSetting.class ).to( DecongestionTollingBangBang.class );
				}
				case PID -> {
					this.bind( DecongestionTollingPID.class ).in( Singleton.class );
					this.bind( DecongestionTollSetting.class ).to( DecongestionTollingPID.class );
					this.addEventHandlerBinding().to( DecongestionTollingPID.class );
				}
				case P_MC -> {
					this.bind( DecongestionTollingP_MCP.class ).in( Singleton.class );
					this.bind( DecongestionTollSetting.class ).to( DecongestionTollingP_MCP.class );
					this.addEventHandlerBinding().to( DecongestionTollingP_MCP.class );
				}
				default -> throw new RuntimeException( "not implemented" );
			}
			
		} else {
			// no pricing
			
		}
		addTravelDisutilityFactoryBinding( TransportMode.car ).to( TollTimeDistanceTravelDisutilityFactory.class );

		this.bind(DecongestionInfo.class).in( Singleton.class );
		
		this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class).in( Singleton.class );
		this.addEventHandlerBinding().to(IntervalBasedTolling.class);
		
		this.addEventHandlerBinding().to(DelayAnalysis.class).in( Singleton.class );
		
		this.addEventHandlerBinding().to(PersonVehicleTracker.class).in( Singleton.class );
		
		this.addControlerListenerBinding().to(DecongestionControlerListener.class).in( Singleton.class );
	}

}

