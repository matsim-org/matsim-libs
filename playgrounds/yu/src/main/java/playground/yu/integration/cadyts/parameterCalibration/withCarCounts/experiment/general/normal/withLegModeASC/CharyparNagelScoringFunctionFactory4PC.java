/* *********************************************************************** *
 * project: org.matsim.*
 * DummyCharyparNagelScoringFunctionFactory4PC.java
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.withLegModeASC;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.paramCorrection.PCCtlListener;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.scoring.ScoringFunctionAccumulator4PC;

public class CharyparNagelScoringFunctionFactory4PC extends
		CharyparNagelScoringFunctionFactory {
	private static String OFFSET_CAR = "offsetCar", OFFSET_PT = "offsetPt",
			OFFSET_WALK = "offsetWalk";
	static double offsetCar, offsetPt, offsetWalk;

	public CharyparNagelScoringFunctionFactory4PC(Config config) {
		super(config.charyparNagelScoring());

		// car
		String offsetCarStr = config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, OFFSET_CAR);
		offsetCar = offsetCarStr == null ? 0d : Double
				.parseDouble(offsetCarStr);

		// pt
		String offsetPtStr = config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, OFFSET_PT);
		offsetPt = offsetPtStr == null ? 0d : Double.parseDouble(offsetPtStr);

		// walk
		String offsetWalkStr = config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, OFFSET_WALK);
		offsetWalk = offsetWalkStr == null ? 0d : Double
				.parseDouble(offsetWalkStr);
	}

	public static double getOffsetCar() {
		return offsetCar;
	}

	public static double getOffsetPt() {
		return offsetPt;
	}

	public static double getOffsetWalk() {
		return offsetWalk;
	}

	public static void setOffsetPt(double offsetPt) {
		CharyparNagelScoringFunctionFactory4PC.offsetPt = offsetPt;
	}

	public static void setOffsetWalk(double offsetWalk) {
		CharyparNagelScoringFunctionFactory4PC.offsetWalk = offsetWalk;
	}

	public static void setOffsetCar(double offsetCar) {
		CharyparNagelScoringFunctionFactory4PC.offsetCar = offsetCar;
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {
		CharyparNagelScoringParameters params = getParams();
		ScoringFunctionAccumulator4PC scoringFunctionAccumulator = new ScoringFunctionAccumulator4PC(
				params);
		scoringFunctionAccumulator
				.addScoringFunction(new ActivityScoringFunction(plan, params));
		scoringFunctionAccumulator
				.addScoringFunction(new LegScoringFunction4PC(plan, params));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(
				params));
		scoringFunctionAccumulator
				.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}
}
