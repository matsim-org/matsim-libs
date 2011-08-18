/* *********************************************************************** *
 * project: org.matsim.*
 * PCPlansScoring.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.fine;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;

public class PCPlansScoring implements StartupListener, ScoringListener,
		IterationStartsListener {
	private final static Logger log = Logger.getLogger(PCPlansScoring.class);
	private PCEventsToScore planScorer;

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();
		planScorer = new PCEventsToScore(controler.getConfig(),
				controler.getScoringFunctionFactory(),
				controler.getPopulation());
		log.debug("ParameterCalibrationPlanScoring loaded ScoringFunctionFactory");
		controler.getEvents().addHandler(planScorer);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		planScorer.reset(event.getIteration());
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		planScorer.finish();
	}

	public PCEventsToScore getPlanScorer() {
		return planScorer;
	}

}
