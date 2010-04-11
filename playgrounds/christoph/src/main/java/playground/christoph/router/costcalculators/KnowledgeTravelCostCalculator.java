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

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;

public class KnowledgeTravelCostCalculator implements PersonalizableTravelCost, Cloneable {

	protected Person person;
	protected TravelTime timeCalculator;
	protected double travelCostFactor;
	protected double marginalUtlOfDistance;
	protected boolean checkNodeKnowledge = true;
	
	protected KnowledgeTools knowledgeTools;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostCalculator.class);
	
	public KnowledgeTravelCostCalculator(TravelTime timeCalculator)
	{
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.travelCostFactor = -Gbl.getConfig().charyparNagelScoring().getTraveling() / 3600.0;
		this.marginalUtlOfDistance = Gbl.getConfig().charyparNagelScoring().getMarginalUtlOfDistanceCar();
		
		knowledgeTools = new KnowledgeTools();
	}
	
	public void setPerson(Person person)
	{
		this.person = person;		
	}
	
	public double getLinkTravelCost(final Link link, final double time) 
	{	
		if (checkNodeKnowledge)
		{
			// try getting NodeKnowledge from the Persons Knowledge
			NodeKnowledge nodeKnowledge = knowledgeTools.getNodeKnowledge(person);
			
			// if the Person doesn't know the link -> return max costs 
			if (!nodeKnowledge.knowsLink(link))
			{
//				log.info("Link is not part of the Persons knowledge!");
				return Double.MAX_VALUE;
			}		
		}
		
		// Person knows the link, so calculate it's costs
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
	
		if (this.marginalUtlOfDistance == 0.0) 
		{
			return travelTime * this.travelCostFactor;
		}
		return travelTime * this.travelCostFactor - this.marginalUtlOfDistance * link.getLength();
	}
	
	public void checkNodeKnowledge(boolean value)
	{
		this.checkNodeKnowledge = value;
	}
	
	@Override
	public KnowledgeTravelCostCalculator clone()
	{	
		TravelTime timeCalculatorClone = null;
		if (timeCalculator instanceof Cloneable)
		{
			try
			{
				Method method;
				method = timeCalculator.getClass().getMethod("clone", new Class[]{});
				timeCalculatorClone = timeCalculator.getClass().cast(method.invoke(timeCalculator, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (timeCalculatorClone == null)
		{
			timeCalculatorClone = timeCalculator;
			log.warn("Could not clone the Travel Time Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		KnowledgeTravelCostCalculator clone = new KnowledgeTravelCostCalculator(timeCalculatorClone);
		clone.marginalUtlOfDistance = this.marginalUtlOfDistance;
		clone.travelCostFactor = this.travelCostFactor;
		clone.checkNodeKnowledge = this.checkNodeKnowledge;
		
		return clone;
	}
}