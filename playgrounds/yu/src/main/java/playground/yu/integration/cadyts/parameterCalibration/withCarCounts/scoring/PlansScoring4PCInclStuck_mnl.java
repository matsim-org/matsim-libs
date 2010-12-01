/* *********************************************************************** *
 * project: org.matsim.*
 * DummyPlansScoring4PC.java
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

/**
 * 
 */
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;

import cadyts.utilities.math.MultinomialLogit;

/**
 * a changed copy of {@code PlansScoring} for the parameter calibration,
 * especially in order to put new parameters to CharyparNagelScoringConfigGroup
 * 
 * @author yu
 * 
 */
public class PlansScoring4PCInclStuck_mnl extends PlansScoring4PC implements
		StartupListener, ScoringListener, IterationStartsListener {
	public void notifyStartup(final StartupEvent event) {
		Controler ctl = event.getControler();
		Config config = ctl.getConfig();

		planScorer = new Events2Score4TravPerfStuck_mnl(createMultinomialLogit(config), ctl
				.getScoringFunctionFactory(), ctl.getPopulation(), config
				.strategy().getMaxAgentPlanMemorySize(), config
				.charyparNagelScoring());

		Logger.getLogger(PlansScoring.class).debug(
				"PlanScoring loaded ScoringFunctionFactory");
		ctl.getEvents().addHandler(planScorer);
	}

	protected MultinomialLogit createMultinomialLogit(Config config) {
		int choiceSetSize = config.strategy().getMaxAgentPlanMemorySize(), //
		attributeCount = Integer.parseInt(config.findParam("bse",
				"attributeCount"));
		CharyparNagelScoringConfigGroup scoringCfg = config
				.charyparNagelScoring();
		double traveling = scoringCfg.getTraveling_utils_hr(), // 
		travelingPt = scoringCfg.getTravelingPt_utils_hr(), //
		performing = scoringCfg.getPerforming_utils_hr(), // 
		brainExpBeta = scoringCfg.getBrainExpBeta(), //
		lateArrival = scoringCfg.getLateArrival_utils_hr();

		// initialize MultinomialLogit
		MultinomialLogit mnl = new MultinomialLogit(choiceSetSize/* 1 */,
				attributeCount);// =3 [travCar,travPt,Perf]
		mnl.setUtilityScale(brainExpBeta);
		for (int i = 0; i < choiceSetSize; i++) {
			mnl.setASC(i, 0);
		}
		mnl.setCoefficient(0, traveling);
		mnl.setCoefficient(1, travelingPt);
		mnl.setCoefficient(2, performing);
		mnl.setCoefficient(3, lateArrival);

		return mnl;
	}
}
