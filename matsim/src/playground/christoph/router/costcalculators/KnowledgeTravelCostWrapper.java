package playground.christoph.router.costcalculators;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.network.LinkIdComparator;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.misc.Time;

import playground.christoph.events.LinkVehiclesCounter;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.KnowledgeTravelCost;

public class KnowledgeTravelCostWrapper extends KnowledgeTravelCost{
	
	protected TravelCost travelCostCalculator;
	protected Map<Link, Double> linkTravelCosts;
	protected boolean useLookupTable = true;
	protected boolean checkNodeKnowledge = true;
	protected double lastUpdate = Time.UNDEFINED_TIME; 
	protected KnowledgeTools knowledgeTools;
	protected LinkVehiclesCounter linkVehiclesCounter;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostWrapper.class);
	
	public KnowledgeTravelCostWrapper(TravelCost travelCost)
	{
		this.travelCostCalculator = travelCost;
		
		LinkIdComparator linkComparator = new LinkIdComparator();
		linkTravelCosts = new TreeMap<Link, Double>(linkComparator);
		
		this.knowledgeTools = new KnowledgeTools();
	}
	
	public void setLinkVehiclesCounter(LinkVehiclesCounter linkVehiclesCounter)
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
	
	@Override
	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		super.setQueueNetwork(queueNetwork);
		if (travelCostCalculator instanceof KnowledgeTravelCost)
		{
			((KnowledgeTravelCost)travelCostCalculator).setQueueNetwork(queueNetwork);
		}
	}

	@Override
	public void setPerson(PersonImpl person)
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
		if (checkNodeKnowledge && !nodeKnowledge.knowsLink((LinkImpl)link))
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
		
		for (QueueLink queueLink : queueNetwork.getLinks().values())
		{
			LinkImpl link = queueLink.getLink();
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
					LinkImpl link = this.queueNetwork.getNetworkLayer().getLink(id);
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
		TravelCost travelCostCalculatorClone;
		
		if(this.travelCostCalculator instanceof KnowledgeTravelCost)
		{
			travelCostCalculatorClone = ((KnowledgeTravelCost)this.travelCostCalculator).clone();
		}
		else if (this.travelCostCalculator instanceof OnlyTimeDependentTravelCostCalculator)
		{
			travelCostCalculatorClone = ((OnlyTimeDependentTravelCostCalculator)this.travelCostCalculator).clone();
		}
		else
		{
			log.error("Could not clone the CostCalculator - use reference to the existing Calculator and hope the best...");
			travelCostCalculatorClone = this.travelCostCalculator;
		}
		
		KnowledgeTravelCostWrapper clone = new KnowledgeTravelCostWrapper(travelCostCalculatorClone);
		clone.useLookupTable = this.useLookupTable;
		clone.linkTravelCosts = this.linkTravelCosts;
		clone.checkNodeKnowledge = checkNodeKnowledge;
		
		return clone;
	}

}
