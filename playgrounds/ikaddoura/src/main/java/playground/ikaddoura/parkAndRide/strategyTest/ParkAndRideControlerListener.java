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
import org.matsim.core.replanning.modules.ReRouteDijkstra;
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

//		macht aus parkAndRide z.B. BikeAndRide bzw. führt die Standard SubtourModeChoice für Pläne ohne ParkAndRide aus.
//		PlanStrategy strategy1 = new PlanStrategyImpl(new RandomPlanSelector());
//		strategy1.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
//		strategy1.addStrategyModule(new SubtourModeChoice(controler.getConfig()));
//		strategy1.addStrategyModule(new ReRoute(controler));
		
		// verändert einen Plan nur wenn ein auto verfügbar ist ("car" in person Id)
		// wenn Plan noch kein ParkAndRide enthält: ParkAndRide wird eingebaut, normaler leg wird in [car-ParkAndRide-pt] bzw. [pt-ParkAndRide-car] umgewandelt 
		// wenn Plan bereits ParkAndRide enthält: zufällige Auswahl einer anderen ParkAndRide Facility
		PlanStrategy strategy2 = new PlanStrategyImpl(new RandomPlanSelector());
		strategy2.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategy2.addStrategyModule(new ParkAndRidePlanStrategyModule(controler));
		strategy2.addStrategyModule(new ReRoute(controler));
		
		// eine SubtourModeChoice-Strategie, welche die parkAndRide Aktivitäten und ptLegs rauswirft
//		PlanStrategy strategy3 = new PlanStrategyImpl(new RandomPlanSelector());
//		strategy3.addStrategyModule(new ParkAndRideRemoverStrategy(controler)); // toDo!!!
//		strategy3.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
//		strategy3.addStrategyModule(new SubtourModeChoice(controler.getConfig()));
//		strategy3.addStrategyModule(new ReRoute(controler)
		
		//außerdem benötigt: eine TransitTimeAllocation, welche nicht die parkAndRide Aktivitäten verschiebt

		StrategyManager manager = this.controler.getStrategyManager() ;
		manager.addStrategy(strategy2, 0.2);
//		manager.addChangeRequest(7, strategy2, 0.0);
	}

}
