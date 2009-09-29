package playground.christoph.router.costcalculators;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkIdComparator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.misc.Time;

import playground.christoph.events.LinkVehiclesCounter2;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.KnowledgeTravelCost;

/*
 * Add two new Features to KnowledgeTravelCost:
 * 1) A LookupTable can be used for the LinkTravelCosts
 * If this Feature is used, a LookupTableUpdater and a
 * LinkVehiclesCounter have to be used and the Network
 * has to be set.
 * 
 * 2) The Knowledge of a Person can be taken in account. That means
 * that it is checked if a Person knows a Link or not. If not, the
 * returned TravelCost is Double.MAX_VALUE. 
 */
public class KnowledgeTravelCostWrapper extends KnowledgeTravelCost{
	
	protected TravelCost travelCostCalculator;
	protected Map<Link, Double> linkTravelCosts;
	protected boolean useLookupTable = true;
	protected boolean checkNodeKnowledge = true;
	protected double lastUpdate = Time.UNDEFINED_TIME; 
	protected KnowledgeTools knowledgeTools;
	protected LinkVehiclesCounter2 linkVehiclesCounter;
	protected Network network;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostWrapper.class);
	
	public KnowledgeTravelCostWrapper(TravelCost travelCost)
	{
		this.travelCostCalculator = travelCost;
		
		LinkIdComparator linkComparator = new LinkIdComparator();
		linkTravelCosts = new TreeMap<Link, Double>(linkComparator);
		
		this.knowledgeTools = new KnowledgeTools();
	}
	
	public void setLinkVehiclesCounter(LinkVehiclesCounter2 linkVehiclesCounter)
	{
		this.linkVehiclesCounter = linkVehiclesCounter;
	}
	
	public void setTravelCostCalculator(TravelCost travelCost)
	{
		this.travelCostCalculator = travelCost;
	}
	
	public TravelCost getTravelCostCalculator()
	{
		return this.travelCostCalculator;
	}
	
	public void checkNodeKnowledge(boolean value)
	{
		this.checkNodeKnowledge = value;
	}
	
	public void useLookupTable(boolean value)
	{
		this.useLookupTable = value;
	}
	
	public void setNetwork(Network network)
	{
		this.network = network;
	}
	
	@Override
	public void setPerson(Person person)
	{
		super.setPerson(person);
		if (travelCostCalculator instanceof KnowledgeTravelCost)
		{
			((KnowledgeTravelCost)travelCostCalculator).setPerson(person);
		}
	}
	
	public double getLinkTravelCost(final Link link, final double time) 
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
//			log.info("Link is part of the Persons knowledge!");
			if(useLookupTable)
			{
				return linkTravelCosts.get(link);
			}
			else 
			{
//				log.info("Get Costs from TravelCostCalculator");
				return travelCostCalculator.getLinkTravelCost(link, time);
			}
		}
	}

	private void createLookupTable(double time)
	{
//		log.info("Creating LookupTable for LinkTravelTimes. Time: " + time);
		resetLookupTable();
		
		for (Link link : network.getLinks().values())
		{
			linkTravelCosts.put(link, travelCostCalculator.getLinkTravelCost(link, time));
		}
//		log.info("Done");
	}
	
	public void updateLookupTable(double time)
	{
		if (useLookupTable)
		{
			if (lastUpdate != time)
			{
				// executed only initially
				if (linkTravelCosts.size() == 0) createLookupTable(time);
				
				// Reset Person so that their Knowledge can't be used
				this.setPerson(null);
				
//				log.info("Updating LookupTable for LinkTravelTimes. Time: " + time);
				Map<Id, Integer> links2Update = linkVehiclesCounter.getChangedLinkVehiclesCounts();
			
				for (Id id : links2Update.keySet())
				{
					Link link = network.getLinks().get(id);
					linkTravelCosts.put(link, travelCostCalculator.getLinkTravelCost(link, time));
				}
				lastUpdate = time;
			}
//			else
//			{
//				log.error("Duplicated LookupTable Update requested... skipped it!");
//			}
//			log.info("Done");
		}
	}
	
	public void resetLookupTable()
	{
//		log.info("Resetting LookupTable");
		linkTravelCosts.clear();
	}

	@Override
	public KnowledgeTravelCostWrapper clone()
	{
//		TravelCost travelCostCalculatorClone;	
//		if(this.travelCostCalculator instanceof KnowledgeTravelCost)
//		{
//			travelCostCalculatorClone = ((KnowledgeTravelCost)this.travelCostCalculator).clone();
//		}
//		else if (this.travelCostCalculator instanceof OnlyTimeDependentTravelCostCalculator)
//		{
//			travelCostCalculatorClone = ((OnlyTimeDependentTravelCostCalculator)this.travelCostCalculator).clone();
//		}
//		else
//		{
//			log.error("Could not clone the CostCalculator - use reference to the existing Calculator and hope the best...");
//			travelCostCalculatorClone = this.travelCostCalculator;
//		}
		
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
		clone.useLookupTable = this.useLookupTable;
		clone.linkTravelCosts = this.linkTravelCosts;
		clone.setNetwork(this.network);
		clone.checkNodeKnowledge = checkNodeKnowledge;
		
		return clone;
	}

}
