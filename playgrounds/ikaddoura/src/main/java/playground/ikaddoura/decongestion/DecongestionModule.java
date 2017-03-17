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

package playground.ikaddoura.decongestion;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

import playground.ikaddoura.decongestion.DecongestionConfigGroup.DecongestionApproach;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;
import playground.ikaddoura.decongestion.handler.IntervalBasedTolling;
import playground.ikaddoura.decongestion.handler.IntervalBasedTollingAll;
import playground.ikaddoura.decongestion.handler.PersonVehicleTracker;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingBangBang;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingPID;

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
		
		if (decongestionConfigGroup.getDecongestionApproach().toString().equals(DecongestionApproach.PID.toString())) {
			this.bind(DecongestionTollingPID.class).asEagerSingleton();
			this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
			this.addEventHandlerBinding().to(DecongestionTollingPID.class);
		
		} else if (decongestionConfigGroup.getDecongestionApproach().toString().equals(DecongestionApproach.BangBang.toString())) {
			this.bind(DecongestionTollingBangBang.class).asEagerSingleton();
			this.bind(DecongestionTollSetting.class).to(DecongestionTollingBangBang.class);
		
		} else {
			throw new RuntimeException("Unknown decongestion pricing approach. Aborting...");
		}
		
		this.bind(DecongestionInfo.class).asEagerSingleton();
		
		this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);
		
		this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
		this.bind(DelayAnalysis.class).asEagerSingleton();
		this.bind(PersonVehicleTracker.class).asEagerSingleton();
						
		this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
		this.addEventHandlerBinding().to(DelayAnalysis.class);
		this.addEventHandlerBinding().to(PersonVehicleTracker.class);
		
		this.addControlerListenerBinding().to(DecongestionControlerListener.class);
	}

}

