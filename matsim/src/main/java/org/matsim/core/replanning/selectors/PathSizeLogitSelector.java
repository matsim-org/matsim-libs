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
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

import javax.inject.Inject;

/**
 * select an existing Plan according to the Path Size Logit (e.g. Frejinger, E. and Bierlaire, M.: Capturing Correlation
 * with subnetworks in route choice models, Transportation Research Part B (2006), doi:10.1016/j.trb.2006.06.003.)
 * If there are unscored plans one of it will be chosen randomly (optimistic strategy).
 *
 * @author laemmel
 */
public final class PathSizeLogitSelector extends AbstractPlanSelector {

	private final double pathSizeLogitExponent;
	private final double logitScaleFactor;
	private Network network;

	public PathSizeLogitSelector( final double pathSizeLogitExponent, final double logitScaleFactor, final Network network ) {
		this.pathSizeLogitExponent = pathSizeLogitExponent ;
		this.logitScaleFactor = logitScaleFactor ;
		this.network = network ;
	}

	public PathSizeLogitSelector(final PlanCalcScoreConfigGroup config, final Network network) {
		this( config.getPathSizeLogitBeta(), config.getBrainExpBeta(), network ) ;
	}

	//updates the path size logit weights
	@Override
	protected Map<Plan,Double> calcWeights(final List<? extends Plan> plans ) {
		// ("plans" is the list of plans of a single person)
		
		Map<Plan,Double> weights = new HashMap<Plan,Double>() ;

		double maxScore = Double.NEGATIVE_INFINITY;

		HashMap<Id<Link>, ArrayList<Double>> linksInTime = new HashMap<>();
		// (a data structure that memorizes possible leg start times for link utilization (??))
		
		HashMap<Integer,Double> planLength = new HashMap<Integer, Double>();
		// (a data structure that memorizes the total travel distance of each plan)
		// (yyyy is it obvious that no two plans can have the same hash code?  what happens if they do?  kai, oct'12)
		// (why not just <Plan,Double>?????)
		
		//this gets the choice sets C_n
		//TODO [GL] since the lack of information in Route(),
		//the very first and the very last link of a path will be ignored - gl
		//dg, 09-2013: as first and last link are equal for all routes between to activities this is no major issue

		for (Plan plan : plans) {

			if (plan.getScore() > maxScore) maxScore = plan.getScore();

			double pathSize = 0;
			double currentEndTime = 0.0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					currentEndTime = leg.getDepartureTime();

					NetworkRoute r = (NetworkRoute) leg.getRoute();
					// (yyyy this will fail when the route is not a network route.  kai, oct'12)

					pathSize += RouteUtils.calcDistanceExcludingStartEndLink(r, network);
					// (i.e. pathSize will be the sum over all routes of the plan)
					
					for (Id<Link> linkId : r.getLinkIds()){
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

		for (Plan plan : plans) {

			double tmp = 0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					double currentTime = leg.getDepartureTime();
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					for (Id<Link> linkId : route.getLinkIds()){
						double denominator = 0;
						for (double dbl : linksInTime.get(linkId)){
							//TODO this is just for testing (those legs where the departure time differs more then 3600 seconds will not compared to each other) - need a
							//little bit to brood on it - gl
							// An alternative might be to use a kernal, e.g. a Gaussian.  Something like
							// denominator += exp( (dbl-currentTime)^2 / sigma^2 ) .  kai, oct'12
							if (Math.abs(dbl - currentTime) <= 3600)
								denominator++;
						}
						// (the meaning seems to be: for each link that the plan uses, it checks how many other times the
						// same link is used by a leg that has roughly the same departure time (*))

						Link link = network.getLinks().get(linkId);
						tmp += link.getLength() / denominator;
						// (for a plan, the weight of a link is divided by the number of times it is used)
					}
				}
			}
			// tmp is now a number that contains the ``reduced'' travel distance of the plan.  Divide it by the full travel distance
			// of the plan, and take to the power of this.beta:
			double PSi = Math.pow(tmp/planLength.get(plan.hashCode()), this.pathSizeLogitExponent);
			
			double weight;
			if (Double.isInfinite(maxScore)) {
				// (isInfinite(x) also returns true when x==-Infinity) 
				
				// likely that wc.maxScore == -Infinity, and thus plan.getScoreAsPrimitiveType() also == -Infinity, handle it like any other case where getScore() == maxScore
				// I do not understand the line above.  kai, oct'12
				
				weight = PSi;
				// (yy I do not understand.  Presumably, wc.maxScore may be -Infinity, in which case ALL plan scores are -Infinity 
				// (or NaN or null or something similar).  In this case, plans are simply weighted by their PSi, so that 
				// overlapping plans get less weight than very different plans. kai, oct'12)
			} else {
				weight = Math.exp(this.logitScaleFactor * (plan.getScore() - maxScore))*PSi;
				// (this is essentially $PSi * exp( tau * score )$, the "-wc.maxScore" is (presumably) the computational trick
				// to avoid overflow)
			}

			if (weight <= 0.0) weight = 0;
			// (yy how can weight become negative?? kai, oct'12) 

			// the weight is memorized; the sum of all weights in computed.  Choice will be based on those weights
			weights.put( plan, weight) ;
		}
		
		return weights ;
	}

}
