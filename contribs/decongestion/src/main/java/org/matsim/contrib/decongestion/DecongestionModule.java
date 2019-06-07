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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;
import org.matsim.contrib.decongestion.handler.IntervalBasedTolling;
import org.matsim.contrib.decongestion.handler.IntervalBasedTollingAll;
import org.matsim.contrib.decongestion.handler.PersonVehicleTracker;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollSetting;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollingBangBang;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollingPID;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollingP_MCP;
import org.matsim.core.controler.AbstractModule;

/**
* @author ikaddoura
*/

public class DecongestionModule extends AbstractModule {

	private final DecongestionConfigGroup decongestionConfigGroup;
	
	public DecongestionModule(Scenario scenario) {
		this.decongestionConfigGroup = (DecongestionConfigGroup) scenario.getConfig().getModules().get(DecongestionConfigGroup.GROUP_NAME);
	}

	@Override
	public void install() {
		
		if (decongestionConfigGroup.isEnableDecongestionPricing()) {
			switch( decongestionConfigGroup.getDecongestionApproach() ) {
				case BangBang:
					this.bind(DecongestionTollingBangBang.class).asEagerSingleton();
					this.bind(DecongestionTollSetting.class).to(DecongestionTollingBangBang.class);
					break;
				case PID:
					this.bind(DecongestionTollingPID.class).asEagerSingleton();
					this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
					this.addEventHandlerBinding().to(DecongestionTollingPID.class);
					break;
				case P_MC:
					this.bind(DecongestionTollingP_MCP.class).asEagerSingleton();
					this.bind(DecongestionTollSetting.class).to(DecongestionTollingP_MCP.class);
					this.addEventHandlerBinding().to(DecongestionTollingP_MCP.class);
					break;
				default:
					throw new RuntimeException("not implemented") ;
			}
			
		} else {
			// no pricing
			
		}
		this.bind(DecongestionInfo.class).asEagerSingleton();
		
		this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
		this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);
		this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
		
		this.bind(DelayAnalysis.class).asEagerSingleton();
		this.addEventHandlerBinding().to(DelayAnalysis.class);
		
		this.addEventHandlerBinding().to(PersonVehicleTracker.class).asEagerSingleton();
		
		this.addControlerListenerBinding().to(DecongestionControlerListener.class);
	}

}

