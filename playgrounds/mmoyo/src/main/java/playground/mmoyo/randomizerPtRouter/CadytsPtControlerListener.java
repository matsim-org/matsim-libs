///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.mmoyo.randomizerPtRouter;
//
//import org.matsim.contrib.cadyts.pt.CadytsPtPlanChanger;
//import org.matsim.contrib.cadyts.pt.CadytsPtPlanStrategy;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.events.StartupEvent;
//import org.matsim.core.controler.listener.StartupListener;
//import org.matsim.core.replanning.PlanStrategy;
//import org.matsim.core.replanning.PlanStrategyImpl;
//import org.matsim.core.replanning.StrategyManager;
//
//class CadytsPtControlerListener implements StartupListener {
//
//	// This class is short and should stay short, since it is just a wrapper to avoid  using the strategy module set-up
//	// via the config file.  kai/manuel, dec'10
//	
//	private Controler controler ;
//
//	public CadytsPtControlerListener( Controler ctl ) {
//		this.controler = ctl ;
//	}
//	
//	@Override
//	public void notifyStartup(final StartupEvent event) {
//		//Controler controler = event.getControler() ;
//		
//		// create the strategy:
//		PlanStrategy strategy = new PlanStrategyImpl(new CadytsPtPlanChanger(context)); ;
//
//		// add the strategy to the strategy manager:
//		System.out.println(" controler.getStrategyManager()" + (controler.getStrategyManager() == null));
//		StrategyManager manager = this.controler.getStrategyManager() ;
//		manager.addStrategy(strategy, 1.0 ) ;
//		
//	}
//	
//}