/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.ikaddoura.parkAndRide.strategyTest;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;

public class ParkAndRideControlerListener implements StartupListener {
	
	Controler controler ;
	
	ParkAndRideControlerListener( Controler ctl ) {
		this.controler = ctl ;
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		PlanStrategy strategy = new PlanStrategyImpl(new RandomPlanSelector());
		// PlanStrategyImpl hat einen PlanSelector, 0 oder mehr StrategyModules
		// der PlanSelector wählt einen der existierenden Pläne
		// StrategyModule verändert einen Plan
		
//		// TransitSubtourModeChoice
//		strategy.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
//		strategy.addStrategyModule(new SubtourModeChoice(controler.getConfig()));
//		strategy.addStrategyModule(new ReRoute(controler));
//		//-------------------------
		
		// Park&Ride
		strategy.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategy.addStrategyModule(new ParkAndRidePlanStrategyModule(controler));
//		// - add Activity: parkAndRide mit zufälligen Koordinaten
//		// - die an HomeActivity liegende Subtour: car mode, alle anderen legs: pt
//		strategy.addStrategyModule(new ReRoute(controler));
		//-------------------------

		StrategyManager manager = this.controler.getStrategyManager() ;
		manager.addStrategy(strategy, 1.0 );
		// Strategie und Auswahlgewichtung werden dem StrategyManager hinzugefügt
	}

}
