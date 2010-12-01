/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeTravelCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.router.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;

public class KnowledgeTravelCostCalculator implements PersonalizableTravelCost {

	protected Person person;
	protected TravelTime timeCalculator;
	protected double travelCostFactor;
	protected double marginalUtlOfDistance;
	protected boolean checkNodeKnowledge = true;

	protected KnowledgeTools knowledgeTools;

	public KnowledgeTravelCostCalculator(TravelTime timeCalculator, CharyparNagelScoringConfigGroup config) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.travelCostFactor = -config.getTraveling_utils_hr() / 3600.0;

//		this.marginalUtlOfDistance = config.getMarginalUtlOfDistanceCar();
		this.marginalUtlOfDistance = config.getMonetaryDistanceCostRateCar() * config.getMarginalUtilityOfMoney() ;

		knowledgeTools = new KnowledgeTools();
	}

	@Override
	public void setPerson(Person person) {
		this.person = person;
	}

	@Override
	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		if (checkNodeKnowledge) {
			// try getting NodeKnowledge from the Persons Knowledge
			NodeKnowledge nodeKnowledge = knowledgeTools.getNodeKnowledge(person);

			// if the Person doesn't know the link -> return max costs
			if (!nodeKnowledge.knowsLink(link)) {
				return Double.MAX_VALUE;
			}
		}

		// Person knows the link, so calculate it's costs
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);

		if (this.marginalUtlOfDistance == 0.0) {
			return travelTime * this.travelCostFactor;
		}
		return travelTime * this.travelCostFactor - this.marginalUtlOfDistance * link.getLength();
	}

	public void checkNodeKnowledge(boolean value) {
		this.checkNodeKnowledge = value;
	}
	
}