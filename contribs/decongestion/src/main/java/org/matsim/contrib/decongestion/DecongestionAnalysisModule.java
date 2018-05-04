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

import org.matsim.core.controler.AbstractModule;

import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;
import org.matsim.contrib.decongestion.handler.PersonVehicleTracker;

/**
* @author ikaddoura
*/

public class DecongestionAnalysisModule extends AbstractModule {
	
	@Override
	public void install() {
				
		this.bind(DecongestionInfo.class).asEagerSingleton();		
		
		this.bind(DelayAnalysis.class).asEagerSingleton();				
		this.addEventHandlerBinding().to(DelayAnalysis.class);

		this.bind(PersonVehicleTracker.class).asEagerSingleton();
		this.addEventHandlerBinding().to(PersonVehicleTracker.class);
		
		this.addControlerListenerBinding().to(DecongestionControlerListener.class);
	}

}

