package playground.christoph.router.costcalculators;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.KnowledgeTravelTime;

/*
 * Adds a new Feature to KnowledgeTravelTime:
 * The Knowledge of a Person can be taken in account. That means
 * that it is checked if a Person knows a Link or not. If not, the
 * returned TravelTime is Double.MAX_VALUE. 
 */
public class KnowledgeTravelTimeWrapper extends KnowledgeTravelTime{
	
	protected TravelTime travelTimeCalculator;
	protected boolean checkNodeKnowledge = true;
	protected KnowledgeTools knowledgeTools;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostWrapper.class);
	
	public KnowledgeTravelTimeWrapper(TravelTime travelTime)
	{
		this.travelTimeCalculator = travelTime;
		
		this.knowledgeTools = new KnowledgeTools();
	}
	
	public void checkNodeKnowledge(boolean value)
	{
		this.checkNodeKnowledge = value;
	}
	
	@Override
	public void setPerson(Person person)
	{
		super.setPerson(person);
		if (travelTimeCalculator instanceof KnowledgeTravelTime)
		{
			((KnowledgeTravelTime)travelTimeCalculator).setPerson(person);
		}
	}
	
	public double getLinkTravelTime(final Link link, final double time) 
	{	
		NodeKnowledge nodeKnowledge = null;
		
		if (checkNodeKnowledge && person != null)
		{
			// try getting NodeKnowledge from the Persons Knowledge
			nodeKnowledge = knowledgeTools.getNodeKnowledge(person);
		}
		
		// if the Person doesn't know the link -> return max costs 
		if (nodeKnowledge != null && !nodeKnowledge.knowsLink(link))
		{
//			log.info("Link is not part of the Persons knowledge!");
			return Double.MAX_VALUE;
		}
		else
		{
//			log.info("Get Times from TravelTimeCalculator");
			return travelTimeCalculator.getLinkTravelTime(link, time);
		}
	}

	@Override
	public KnowledgeTravelTimeWrapper clone()
	{		
		TravelTime travelTimeCalculatorClone = null;
		if (travelTimeCalculator instanceof Cloneable)
		{
			try
			{
				Method method;
				method = travelTimeCalculator.getClass().getMethod("clone", new Class[]{});
				travelTimeCalculatorClone = travelTimeCalculator.getClass().cast(method.invoke(travelTimeCalculator, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelTimeCalculatorClone == null)
		{
			travelTimeCalculatorClone = travelTimeCalculator;
			log.warn("Could not clone the Travel Time Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		KnowledgeTravelTimeWrapper clone = new KnowledgeTravelTimeWrapper(travelTimeCalculatorClone);
		clone.checkNodeKnowledge = this.checkNodeKnowledge;
		
		return clone;
	}
}
