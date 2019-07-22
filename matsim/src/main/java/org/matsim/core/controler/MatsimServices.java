
/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimServices.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.controler;


import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Provider;

public interface MatsimServices extends IterationCounter {

	IterationStopWatch getStopwatch();

	TravelTime getLinkTravelTimes();

	Provider<TripRouter> getTripRouterProvider();

	TravelDisutility createTravelDisutilityCalculator();

	LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory();

	ScoringFunctionFactory getScoringFunctionFactory();

	Config getConfig();

	Scenario getScenario();

	EventsManager getEvents();

	com.google.inject.Injector getInjector();

	CalcLinkStats getLinkStats();

	VolumesAnalyzer getVolumes();

	ScoreStats getScoreStats();

	TravelDisutilityFactory getTravelDisutilityFactory();

	StrategyManager getStrategyManager();

	OutputDirectoryHierarchy getControlerIO();

	void addControlerListener(ControlerListener controlerListener);
	
}
