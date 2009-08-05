package playground.christoph.router.costcalculators;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.network.LinkIdComparator;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.mobsim.MyQueueNetwork;
import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.KnowledgeTravelTime;

public class KnowledgeTravelTimeWrapper extends KnowledgeTravelTime{
	
	protected TravelTime travelTimeCalculator;
	protected Map<Link, Double> linkTravelTimes;
	protected boolean useLookupTable = true;
	protected boolean checkNodeKnowledge = true;
	protected double lastUpdate = Time.UNDEFINED_TIME; 
	protected KnowledgeTools knowledgeTools;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostWrapper.class);
	
	public KnowledgeTravelTimeWrapper(TravelTime travelTime)
	{
		this.travelTimeCalculator = travelTime;
		
		LinkIdComparator linkComparator = new LinkIdComparator();
		this.linkTravelTimes = new TreeMap<Link, Double>(linkComparator);
		
		this.knowledgeTools = new KnowledgeTools();
	}
	
	public void setTravelTimeCalculator(TravelTime travelTime)
	{
		this.travelTimeCalculator = travelTime;
	}
	
	public TravelTime getTravelTimeCalculator()
	{
		return this.travelTimeCalculator;
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
	public void setMyQueueNetwork(MyQueueNetwork myQueueNetwork)
	{
		super.setMyQueueNetwork(myQueueNetwork);
		if (travelTimeCalculator instanceof KnowledgeTravelTime)
		{
			((KnowledgeTravelTime)travelTimeCalculator).setMyQueueNetwork(myQueueNetwork);
		}
	}

	@Override
	public void setPerson(PersonImpl person)
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
		if (nodeKnowledge != null && !nodeKnowledge.knowsLink((LinkImpl)link))
		{
//			log.info("Link is not part of the Persons knowledge!");
			return Double.MAX_VALUE;
		}
		else
		{
//			log.info("Link is part of the Persons knowledge!");
			if(useLookupTable)
			{
				return linkTravelTimes.get(link);
			}
			else
			{
//				log.info("Get Times from TravelTimeCalculator");
				return travelTimeCalculator.getLinkTravelTime(link, time);
			}
		}
	}

	private void createLookupTable(double time)
	{
//		log.info("Creating LookupTable for LinkTravelTimes. Time: " + time);
		resetLookupTable();
		
		for (QueueLink queueLink : myQueueNetwork.getLinks().values())
		{
			LinkImpl link = queueLink.getLink();
			linkTravelTimes.put(link, travelTimeCalculator.getLinkTravelTime(link, time));
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
				if (linkTravelTimes.size() == 0) createLookupTable(time);
				
				// Reset Person so that their Knowledge can't be used
				this.setPerson(null);
				
//				log.info("Updating LookupTable for LinkTravelTimes. Time: " + time);
				Map<Id, Integer> links2Update = this.myQueueNetwork.getLinkVehiclesCounter().getChangedLinkVehiclesCounts();
			
				for (Id id : links2Update.keySet())
				{
					LinkImpl link = this.myQueueNetwork.getNetworkLayer().getLink(id);
					linkTravelTimes.put(link, travelTimeCalculator.getLinkTravelTime(link, time));
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
		linkTravelTimes.clear();
	}
	

	@Override
	public KnowledgeTravelTimeWrapper clone()
	{
		TravelTime travelTimeCalculatorClone;
		
		if(this.travelTimeCalculator instanceof KnowledgeTravelTime)
		{
			travelTimeCalculatorClone = ((KnowledgeTravelTime)this.travelTimeCalculator).clone();
		}
		else
		{
			log.error("Could not clone the TimeCalculator - use reference to the existing Calculator and hope the best...");
			travelTimeCalculatorClone = this.travelTimeCalculator;
		}
		
		KnowledgeTravelTimeWrapper clone = new KnowledgeTravelTimeWrapper(travelTimeCalculatorClone);
		clone.useLookupTable = this.useLookupTable;
		clone.linkTravelTimes = this.linkTravelTimes;
		clone.checkNodeKnowledge = this.checkNodeKnowledge;
		
		return clone;
	}
}
