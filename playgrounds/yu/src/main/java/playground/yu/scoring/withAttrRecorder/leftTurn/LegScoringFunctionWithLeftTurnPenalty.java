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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.yu.scoring.withAttrRecorder.LegScoringFunctionWithAttrRecorder;
import playground.yu.utils.LeftTurnIdentifier;

/**
 * adds the penalty for the left turns (just for experiments for creating of
 * some utility parameters that don't correlate with each other)
 * 
 * @author yu
 * 
 */
public class LegScoringFunctionWithLeftTurnPenalty extends
		LegScoringFunctionWithAttrRecorder {

	private int nbOfLeftTurnAttrCar = 0;
	protected final AdditionalScoringParameters additionalParams;

	public LegScoringFunctionWithLeftTurnPenalty(
			// Plan plan,
			CharyparNagelScoringParameters params, Network network,
			AdditionalScoringParameters additionalParams) {
		super(
		// plan,
				params, network);
		this.additionalParams = additionalParams;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			Leg leg) {
		int nbOfLeftTurn = LeftTurnIdentifier.getNumberOfLeftTurnsFromALeg(leg,
				network.getLinks());
		nbOfLeftTurnAttrCar += nbOfLeftTurn;
		double originalScore = super.calcLegScore(departureTime, arrivalTime,
				leg);
		double tmpScore = originalScore + additionalParams.constantLeftTurn * // nbOfLeftTurnAttrCar
				nbOfLeftTurn;
		// 这里用nbOfLeftTurnAttrCar还是LeftTurnIdentifier.getNumberOfLeftTurnsFromALeg(leg,
		// network.getLinks())？即，刚刚的+=对结果的影响？此处应该仅仅是此处的leftturn被计入分数，oder？

		// should this also be done by xxxxxxxxxthis class namexxxxxxxxPC?

		// System.out.println(">>>>>\tconstantLeftTurn:\t"
		// + additionalParams.constantLeftTurn
		// + "\t*\tnbOfLeftTurnAttrCar:\t" + nbOfLeftTurnAttrCar
		// + "\toriginalScore:\t" + originalScore + "\tsum:\t" + score);
		return tmpScore;
	}

	public int getNbOfLeftTurnAttrCar() {
		return nbOfLeftTurnAttrCar;
	}

	@Override
	public void reset() {
		super.reset();
		nbOfLeftTurnAttrCar = 0;
	}

}
