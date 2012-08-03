/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;

import playground.yu.scoring.withAttrRecorder.ScoringFunctionAccumulatorWithAttrRecorder;

/**
 * @author yu
 * 
 */
public class ScoringFunctionAccumulatorWithLeftTurnPenalty extends
		ScoringFunctionAccumulatorWithAttrRecorder {
	private int nbOfLeftTurnAttrCar;

	public ScoringFunctionAccumulatorWithLeftTurnPenalty(
			CharyparNagelScoringParameters params) {
		super(params);
	}

	public int getNbOfLeftTurnAttrCar() {
		return nbOfLeftTurnAttrCar;
	}

	@Override
	public double getScore() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			if (basicScoringFunction instanceof CharyparNagelLegScoring) {
				LegScoringFunctionWithLeftTurnPenalty legScoringFunction = (LegScoringFunctionWithLeftTurnPenalty) basicScoringFunction;
				nbOfLeftTurnAttrCar = legScoringFunction
						.getNbOfLeftTurnAttrCar();
			}
		}
		return super.getScore();
	}

}
