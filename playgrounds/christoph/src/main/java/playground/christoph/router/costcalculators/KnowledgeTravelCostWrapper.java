package playground.christoph.router.costcalculators;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelCost;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;

/*
 * Adds s new Feature to KnowledgeTravelCost:
 * The Knowledge of a Person can be taken in account. That means
 * that it is checked if a Person knows a Link or not. If not, the
 * returned TravelCost is Double.MAX_VALUE. 
 */
public class KnowledgeTravelCostWrapper implements PersonalizableTravelCost, Cloneable{
	
	protected Person person;
	protected TravelCost travelCostCalculator;
	protected boolean checkNodeKnowledge = true;
	protected KnowledgeTools knowledgeTools;
	protected Network network;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostWrapper.class);
	
	public KnowledgeTravelCostWrapper(TravelCost travelCost)
	{
		this.travelCostCalculator = travelCost;		
		this.knowledgeTools = new KnowledgeTools();
	}
		
	public void checkNodeKnowledge(boolean value)
	{
		this.checkNodeKnowledge = value;
	}
	
	@Override
	public void setPerson(Person person)
	{
		this.person = person;
		if (travelCostCalculator instanceof PersonalizableTravelCost)
		{
			((PersonalizableTravelCost)travelCostCalculator).setPerson(person);
		}
	}
	
	public double getLinkGeneralizedTravelCost(final Link link, final double time) 
	{	
		NodeKnowledge nodeKnowledge = null;
		if (checkNodeKnowledge && person != null)
		{
			// try getting NodeKnowledge from the Persons Knowledge
			nodeKnowledge = knowledgeTools.getNodeKnowledge(person);
		}		
		
		// if the Person doesn't know the link -> return max costs 
		if (checkNodeKnowledge && !nodeKnowledge.knowsLink(link))
		{
//			log.info("Link is not part of the Persons knowledge!");
			return Double.MAX_VALUE;
		}
		else
		{
//			log.info("Get Costs from TravelCostCalculator");
			return travelCostCalculator.getLinkGeneralizedTravelCost(link, time);
		}
	}
	
	@Override
	public KnowledgeTravelCostWrapper clone()
	{
		TravelCost travelCostCalculatorClone = null;
		if (travelCostCalculator instanceof Cloneable)
		{
			try
			{
				Method method;
				method = travelCostCalculator.getClass().getMethod("clone", new Class[]{});
				travelCostCalculatorClone = travelCostCalculator.getClass().cast(method.invoke(travelCostCalculator, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelCostCalculatorClone == null)
		{
			travelCostCalculatorClone = travelCostCalculator;
			log.warn("Could not clone the Travel Cost Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		KnowledgeTravelCostWrapper clone = new KnowledgeTravelCostWrapper(travelCostCalculatorClone);
		clone.checkNodeKnowledge = checkNodeKnowledge;
		
		return clone;
	}

}
