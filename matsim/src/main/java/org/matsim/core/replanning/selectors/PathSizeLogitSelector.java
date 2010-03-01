/* *********************************************************************** *
 * project: org.matsim.*
 * PathSizeLogitSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.selectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * select an existing Plan according to the Path Size Logit (e.g. Frejinger, E. and Bierlaire, M.: Capturing Correlation
 * with subnetworks in route choice models, Transportation Research Part B (2006), doi:10.1016/j.trb.2006.06.003.)
 * If there are unscored plans one of it will be chosen randomly (optimistic strategy).
 *
 * @author laemmel
  */
public class PathSizeLogitSelector implements PlanSelector {

	private final double beta;
	private final double tau;
	private final Network network;

	public PathSizeLogitSelector(final Network network) {
		this.beta = Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "PathSizeLogitBeta"));

		//in PSL tau is  the equivalent to BrainExpBeta in the multinomial logit model
		this.tau = Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "BrainExpBeta"));

		this.network = network;
	}

	public Plan selectPlan(final Person person) {

		// First check if there are any unscored plans
		Plan selectedPlan = ((PersonImpl) person).getRandomUnscoredPlan();
		if (selectedPlan != null) return selectedPlan;
		// Okay, no unscored plans...

		// Build the weights of all plans

		// - now calculate the weights
		WeightsContainer wc = new WeightsContainer(person.getPlans());
		calcPSLWeights(person.getPlans(), wc);

		// choose a random number over interval [0,sumWeights[
		double selnum = wc.sumWeights*MatsimRandom.getRandom().nextDouble();
		int idx = 0;
		for (Plan plan : person.getPlans()) {
			selnum -= wc.weights[idx];
			if (selnum <= 0.0) {
				((PersonImpl) person).setSelectedPlan(plan);
				return plan;
			}
			idx++;
		}

		// this case should never happen, except a person has no plans at all.
		return null;
	}

	//updates the path size logit weights
	private void calcPSLWeights(final List<? extends Plan> plans, final WeightsContainer wc) {

		wc.maxScore = Double.NEGATIVE_INFINITY;

		HashMap<Id, ArrayList<Double>> linksInTime = new HashMap<Id, ArrayList<Double>>();
		HashMap<Integer,Double> planLength = new HashMap<Integer, Double>();
		//this gets the choice sets C_n
		//TODO [GL] since the lack of information in Route(),
		//the very first and the very last link of a path will be ignored - gl
		for (Plan plan : plans) {

			if (plan.getScore() > wc.maxScore) wc.maxScore = plan.getScore();

			double pathSize = 0;
			double currentEndTime = 0.0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof LegImpl) {
					LegImpl leg = (LegImpl) pe;
					currentEndTime = leg.getDepartureTime();
					NetworkRoute r = (NetworkRoute) leg.getRoute();
					pathSize += RouteUtils.calcDistance(r, this.network);
					for (Id linkId : r.getLinkIds()){
						ArrayList<Double> lit = linksInTime.get(linkId);
						if (lit == null){
							lit = new ArrayList<Double>();
							linksInTime.put(linkId, lit);
						}
						lit.add(currentEndTime);
					}
				}
			}
			planLength.put(plan.hashCode(), pathSize);
		}

		double sumweight = 0;
		int idx = 0;
		for (Plan plan : plans){

			double tmp = 0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof LegImpl) {
					LegImpl leg = (LegImpl) pe;
					double currentTime = leg.getDepartureTime();
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					for (Id linkId : route.getLinkIds()){
						double denominator = 0;
						for (double dbl : linksInTime.get(linkId)){
							//TODO this is just for testing (those legs where the depature time differs more then 3600 seconds will not compared to each other) - need a
							//little bit to brood on it - gl
							if (Math.abs(dbl - currentTime) <= 3600)
								denominator++;
						}
						Link link = this.network.getLinks().get(linkId);
						tmp += link.getLength() / denominator;
					}
				}
			}
			double PSi = Math.pow(tmp/planLength.get(plan.hashCode()), this.beta);
			double weight;
			if (Double.isInfinite(wc.maxScore)) {
				// likely that wc.maxScore == -Infinity, and thus plan.getScoreAsPrimitiveType() also == -Infinity, handle it like any other case where getScore() == maxScore
				weight = PSi;
			} else {
				weight = Math.exp(this.tau * (plan.getScore() - wc.maxScore))*PSi;
			}
			if (weight <= 0.0) weight = 0;
			wc.weights[idx] = weight;
			sumweight += weight;
			idx++;
		}
		wc.sumWeights = sumweight;
	}

	private static class WeightsContainer {
		protected double[] weights;
		protected double sumWeights;
		protected double maxScore;
		protected WeightsContainer(final List<? extends Plan> plans) {
			this.weights = new double[plans.size()];
		}
	}
}
