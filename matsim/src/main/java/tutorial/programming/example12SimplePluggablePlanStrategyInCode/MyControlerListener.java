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
package tutorial.programming.example12SimplePluggablePlanStrategyInCode;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;

/**
 * @author nagel
 *
 */
public class MyControlerListener implements StartupListener {
	
	Controler controler ;
	
	MyControlerListener( Controler ctl ) {
		this.controler = ctl ;
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		PlanStrategy strategy = new PlanStrategyImpl(new RandomPlanSelector() ) ;
		strategy.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()) ) ;
		// ...
		
		// see examples in StrategyManagerConfigLoader
		
		StrategyManager manager = this.controler.getStrategyManager() ;
		manager.addStrategy(strategy, 0.1 ) ;
	}

}
