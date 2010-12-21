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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.config.PtCountsConfigGroup;
import org.matsim.pt.counts.PtCountSimComparisonKMLWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.NewPtBsePlanStrategy;
import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.analysis.PtBseCountsComparisonAlgorithm;
import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.analysis.PtBseOccupancyAnalyzer;

class NewPtBseUCControlerListener implements StartupListener {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( NewPtBseUCControlerListener.class );

	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler() ;
		
		// create the strategy:
		PlanStrategy strategy = new NewPtBsePlanStrategy( controler) ;

		// add the strategy to the strategy manager:
		controler.getStrategyManager().addStrategy( strategy , 1.0 ) ;
		
	}


	
}