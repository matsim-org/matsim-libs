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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
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
public final class PathSizeLogitSelector implements PlanSelector {

	private final double beta;
	private final double tau;
	private final Network network;

	public PathSizeLogitSelector(final Network network, final PlanCalcScoreConfigGroup config) {
		this.beta = config.getPathSizeLogitBeta();

		//in PSL tau is  the equivalent to BrainExpBeta in the multinomial logit model
		this.tau = config.getBrainExpBeta();

		this.network = network;
	}

	@Override
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
	void calcPSLWeights(final List<? extends Plan> plans, final WeightsContainer wc) {
		// ("plans" is the list of plans of a single person)

		wc.maxScore = Double.NEGATIVE_INFINITY;

		HashMap<Id, ArrayList<Double>> linksInTime = new HashMap<Id, ArrayList<Double>>();
		// (a data structure that memorizes possible leg start times for link utilization (??))
		
		HashMap<Integer,Double> planLength = new HashMap<Integer, Double>();
		// (a data structure that memorizes the total travel distance of each plan)
		// (yyyy is it obvious that no two plans can have the same hash code?  what happens if they do?  kai, oct'12)
		
		//this gets the choice sets C_n
		//TODO [GL] since the lack of information in Route(),
		//the very first and the very last link of a path will be ignored - gl
		for (Plan plan : plans) {

			if (plan.getScore() > wc.maxScore) wc.maxScore = plan.getScore();

			double pathSize = 0;
			double currentEndTime = 0.0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					currentEndTime = leg.getDepartureTime();

					NetworkRoute r = (NetworkRoute) leg.getRoute();
					// (yyyy this will fail when the route is not a network route.  kai, oct'12)

					pathSize += RouteUtils.calcDistance(r, this.network);
					// (i.e. pathSize will be the sum over all routes of the plan)
					
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
		for (Plan plan : plans) {

			double tmp = 0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					double currentTime = leg.getDepartureTime();
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					for (Id linkId : route.getLinkIds()){
						double denominator = 0;
						for (double dbl : linksInTime.get(linkId)){
							//TODO this is just for testing (those legs where the departure time differs more then 3600 seconds will not compared to each other) - need a
							//little bit to brood on it - gl
							if (Math.abs(dbl - currentTime) <= 3600)
								denominator++;
						}
						// (the meaning seems to be: for each link that the plan uses, it checks how many other times the
						// same link is used by a leg that has roughly the same departure time (*))

						Link link = this.network.getLinks().get(linkId);
						tmp += link.getLength() / denominator;
						// (for a plan, the weight of a link is divided by the number of times it is used)
					}
				}
			}
			// tmp is now a number that contains the ``reduced'' travel distance of the plan.  Divide it by the full travel distance
			// of the plan, and take to the power of this.beta:
			double PSi = Math.pow(tmp/planLength.get(plan.hashCode()), this.beta);
			
			double weight;
			if (Double.isInfinite(wc.maxScore)) {
				// (isInfinite(x) also returns true when x==-Infinity) 
				
				// likely that wc.maxScore == -Infinity, and thus plan.getScoreAsPrimitiveType() also == -Infinity, handle it like any other case where getScore() == maxScore
				// I do not understand the line above.  kai, oct'12
				
				weight = PSi;
				// (yy I do not understand.  Presumably, wc.maxScore may be -Infinity, in which case ALL plan scores are -Infinity 
				// (or NaN or null or something similar).  In this case, plans are simply weighted by their PSi, so that 
				// overlapping plans get less weight than very different plans. kai, oct'12)
			} else {
				weight = Math.exp(this.tau * (plan.getScore() - wc.maxScore))*PSi;
				// (this is essentially $PSi * exp( tau * score )$, the "-wc.maxScore" is (presumably) the computational trick
				// to avoid overflow)
			}

			if (weight <= 0.0) weight = 0;
			// (yy how can weight become negative?? kai, oct'12) 

			// the weight is memorized; the sum of all weights in computed.  Choice will be based on those weights
			wc.weights[idx] = weight;
			sumweight += weight;
			idx++;
		}
		wc.sumWeights = sumweight;
	}

	class WeightsContainer {
		double[] weights;
		double sumWeights;
		double maxScore;
		WeightsContainer(final List<? extends Plan> plans) {
			this.weights = new double[plans.size()];
		}
	}
}
