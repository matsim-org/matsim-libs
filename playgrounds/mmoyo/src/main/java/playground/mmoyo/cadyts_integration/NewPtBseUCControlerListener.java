/* *********************************************************************** *
 * project: org.matsim.*
 * NewPtBseUCControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mmoyo.cadyts_integration;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;

import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.NewPtBsePlanStrategy;

class NewPtBseUCControlerListener implements StartupListener {

	// This class is short and should stay short, since it is just a wrapper to avoid  using the strategy module set-up
	// via the config file.  kai/manuel, dec'10
	
	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler() ;
		
		// create the strategy:
		PlanStrategy strategy = new NewPtBsePlanStrategy( controler) ;

		// add the strategy to the strategy manager:
		controler.getStrategyManager().addStrategy( strategy , 1.0 ) ;
		
	}


	
}