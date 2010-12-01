/* *********************************************************************** *
 * project: org.matsim.*
 * MnlChoice.java
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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
import playground.yu.utils.DebugTools;
import cadyts.utilities.math.MultinomialLogit;
import cadyts.utilities.math.Vector;

/**
 * @author yu
 * 
 */
public class Events2Score4TravPerf_mnl extends Events2Score4TravPerf implements
		MultinomialLogitChoice {
	protected MultinomialLogit mnl;

	public Events2Score4TravPerf_mnl(MultinomialLogit mnl,
			ScoringFunctionFactory sfFactory, Population pop,
			int maxPlansPerAgent, CharyparNagelScoringConfigGroup scoring) {
		super(pop, sfFactory, scoring.getLearningRate(), maxPlansPerAgent,
				scoring);
		this.mnl = mnl;
	}

	public MultinomialLogit getMultinomialLogit() {
		return mnl;
	}

	public void setMultinomialLogit(MultinomialLogit mnl) {
		this.mnl = mnl;
	}

	/**
	 * set Attr. and Utility (not the score in MATSim) of plans of a person.
	 * This method should be called after removedPlans, i.e. there should be
	 * only choiceSetSize plans in the memory of an agent.
	 * 
	 * @param person
	 * @param travelingCarStats
	 * @param travelingPtStats
	 */
	public void setPersonAttrs(Person person) {
		Id pId = person.getId();
		Map<Plan, Double> legDurMapCar = legDursCar.get(pId), legDurMapPt = legDursPt
				.get(pId), actAttrMap = actAttrs.get(pId);
		if (legDurMapCar == null || legDurMapPt == null || actAttrMap == null) {
			throw new NullPointerException("BSE:\t\twasn't person\t" + pId
					+ "\tsimulated?????");
		}

		List<? extends Plan> plans = person.getPlans();
		// if (plans.size() <= this.mnl.getChoiceSetSize()) {
		for (Entry<Plan, Double> entry : legDurMapCar.entrySet()) {
			Plan pl = entry.getKey();
			Double legDurCar = entry.getValue(), legDurPt = legDurMapPt.get(pl), actDur = actAttrMap
					.get(pl);
			if (legDurCar == null || legDurPt == null || actDur == null) {
				throw new NullPointerException(
						"BSE:\t\tvergot to save some attr?");
			}

			int choiceIdx = plans.indexOf(pl);
			if (plans.size() <= maxPlansPerAgent)/* with mnl */{
				if (choiceIdx < 0 || choiceIdx >= mnl.getChoiceSetSize()) {
					throw new RuntimeException(
							Events2Score4TravPerf_mnl.class.getName()
									+ "\tline:\t"
									+ DebugTools.getLineNumber(new Exception())
									+ "\t"
									+ "IndexOutofBound, choiceIdx<0 or >=choiceSetSize!");
				}

				// set attributes to MultinomialLogit
				mnl.setAttribute(choiceIdx, 0, legDurCar);// traveling
				mnl.setAttribute(choiceIdx, 1, legDurPt);// travelingPt
				mnl.setAttribute(choiceIdx, 2, actDur);// performing
				// /////////////////////////////////////////////////////////////
				if (Double.isNaN(legDurCar) || Double.isNaN(legDurPt)
						|| Double.isNaN(actDur)) {
					throw new RuntimeException(Events2Score4TravPerf_mnl.class
							.getName()
							+ "\tline:\t"
							+ DebugTools.getLineNumber(new Exception())
							+ "\tattr NaN:\nlegDurCar\tlegDurPt\tactDur\n"
							+ legDurCar + "\t" + legDurPt + "\t" + actDur);
				}
				// ///////////////////////////////////////////////////////////////
			}
		}
	}

	public void setPersonScore(Person person) {
		Id psId = person.getId();
		Map<Plan, Double> legDurMapCar = legDursCar.get(psId), legDurMapPt = legDursPt
				.get(psId), actAttrMap = actAttrs.get(psId), stuckScoreMap = stuckAttrs
				.get(psId);
		if (legDurMapCar == null || legDurMapPt == null || actAttrMap == null) {
			throw new NullPointerException("BSE:\t\twasn't person\t" + psId
					+ "\tsimulated?????");
		}

		// int plansSize = person.getPlans().size();

		for (Entry<Plan, Double> entry : legDurMapCar.entrySet()) {
			Plan pl = entry.getKey();
			Double legDurCar = entry.getValue(), legDurPt = legDurMapPt.get(pl), actAttr = actAttrMap
					.get(pl), stuckScore = stuckScoreMap.get(pl);
			if (legDurCar == null || legDurPt == null || actAttr == null) {
				throw new NullPointerException(
						"BSE:\t\tforgot to save some attr?");
			}

			// calculate utility of the plan
			double util = mnl.getCoeff()
			/* travCar,travPt,perf */.innerProd(
					new Vector(legDurCar/* travCar */, legDurPt/* travPt */,
							actAttr/* perf */))
					+ stuckScore;

			if (Double.isNaN(util)) {
				throw new RuntimeException(Events2Score4TravPerf_mnl.class
						.getName()
						+ "\t"
						+ DebugTools.getLineNumber(new Exception())
						+ "\tutil/score is a NaN. legDurCar\t=\t"
						+ legDurCar
						+ "\tlegDurPt\t=\t"
						+ legDurPt
						+ "\tactDur\t=\t"
						+ actAttr + "\nmnl_coeff\t" + mnl.getCoeff());
			}
			pl.setScore(util);
		}
	}
}
