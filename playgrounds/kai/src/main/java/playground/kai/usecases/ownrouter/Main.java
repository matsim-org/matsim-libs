/* *********************************************************************** *
 * project: kai
 * KaiControler.java
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

package playground.kai.usecases.ownrouter;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class Main {

	public static void main(String[] args) {
		final Controler controler = new Controler( "examples/config/daganzo-config.xml" ) ;
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent controlerEvent) {
				PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector()) ;
				KnRouter knRouter = new KnRouter() ;
				strategy.addStrategyModule( knRouter ) ;
				controler.getEvents().addHandler( knRouter ) ;
				
				controler.getStrategyManager().addStrategyForDefaultSubpopulation(strategy, 0.1 ) ;
			}
		}) ;
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}

}
