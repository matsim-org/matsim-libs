package playground.christoph.router.costcalculators;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.LinkIdComparator;
import org.matsim.core.router.util.TravelCost;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.KnowledgeTravelCost;

public class KnowledgeTravelCostWrapper extends KnowledgeTravelCost{
	
	protected TravelCost travelCostcalculator;
	protected Map<LinkImpl, Double> linkTravelCosts;
	protected boolean useLookupTable = true;
	protected boolean updateLookupTable = false;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostWrapper.class);
	
	public KnowledgeTravelCostWrapper(TravelCost travelCost)
	{
		this.travelCostcalculator = travelCost;
	}
	
	public void setTravelCostCalculator(TravelCost travelCost)
	{
		this.travelCostcalculator = travelCost;
	}
	
	public TravelCost getTravelCostCalculator()
	{
		return this.travelCostcalculator;
	}
	
	public double getLinkTravelCost(final LinkImpl link, final double time) 
	{
		// try getting NodeKnowledge from the Persons Knowledge
		NodeKnowledge nodeKnowledge = KnowledgeTools.getNodeKnowledge(person);
		
		// if the Person doesn't know the link -> return max costs 
		if (!nodeKnowledge.knowsLink(link))
		{
//			log.info("Link is not part of the Persons knowledge!");
			return Double.MAX_VALUE;
		}
		else
		{
//			log.info("Link is part of the Persons knowledge!");
			if(useLookupTable)
			{
				if (linkTravelCosts == null) createLookupTable(time);
			
				if (updateLookupTable) updateLookupTable(time);
				
				return linkTravelCosts.get(link);
			}
			else return travelCostcalculator.getLinkTravelCost(link, time);
		}
	}
	

	private void createLookupTable(double time)
	{
//		log.info("Creating LookupTable f�r LinkTravelTimes. Time = " + time);
		LinkIdComparator linkComparator = new LinkIdComparator();
		linkTravelCosts = new TreeMap<LinkImpl, Double>(linkComparator);
		
		for (QueueLink queueLink : myQueueNetwork.getLinks().values())
		{
			LinkImpl link = queueLink.getLink();
			linkTravelCosts.put(link, travelCostcalculator.getLinkTravelCost(link, time));
		}
	}
	
	private void updateLookupTable(double time)
	{
		Map<Id, Integer> links2Update = this.myQueueNetwork.getLinkVehiclesCounter().getChangedLinkVehiclesCounts();
		
		for (Id id : links2Update.keySet())
		{
			LinkImpl link = this.myQueueNetwork.getNetworkLayer().getLink(id);
			linkTravelCosts.put(link, travelCostcalculator.getLinkTravelCost(link, time));
		}
	}
	
	public void updateLookupTable()
	{
		updateLookupTable = true;
	}
	
	public void resetLookupTable()
	{
//		log.info("Resetting LookupTable");
		linkTravelCosts = null;
	}
	

	@Override
	public KnowledgeTravelCostWrapper clone()
	{
		TravelCost travelCostCalculatorClone;
		
		if(this.travelCostcalculator instanceof KnowledgeTravelCost)
		{
			travelCostCalculatorClone = ((KnowledgeTravelCost)this.travelCostcalculator).clone();
		}
		else
		{
			log.error("Could not clone the CostCalculator - use reference to the existing Calculator and hope the best...");
			travelCostCalculatorClone = this.travelCostcalculator;
		}
		
		KnowledgeTravelCostWrapper clone = new KnowledgeTravelCostWrapper(travelCostCalculatorClone);

		return clone;
	}


}
