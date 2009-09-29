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
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.events.LinkVehiclesCounter2;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.KnowledgeTravelTime;

/*
 * Add two new Features to KnowledgeTravelTime:
 * 1) A LookupTable can be used for the LinkTravelTimes
 * If this Feature is used, a LookupTableUpdater and a
 * LinkVehiclesCounter have to be used and the Network
 * has to be set.
 * 
 * 2) The Knowledge of a Person can be taken in account. That means
 * that it is checked if a Person knows a Link or not. If not, the
 * returned TravelTime is Double.MAX_VALUE. 
 */
public class KnowledgeTravelTimeWrapper extends KnowledgeTravelTime{
	
	protected TravelTime travelTimeCalculator;
	protected Map<Link, Double> linkTravelTimes;
	protected boolean useLookupTable = true;
	protected boolean checkNodeKnowledge = true;
	protected double lastUpdate = Time.UNDEFINED_TIME; 
	protected KnowledgeTools knowledgeTools;
	protected LinkVehiclesCounter2 linkVehiclesCounter;
	protected Network network;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostWrapper.class);
	
	public KnowledgeTravelTimeWrapper(TravelTime travelTime)
	{
		this.travelTimeCalculator = travelTime;
		
		LinkIdComparator linkComparator = new LinkIdComparator();
		this.linkTravelTimes = new TreeMap<Link, Double>(linkComparator);
		
		this.knowledgeTools = new KnowledgeTools();
	}
	
	public void setLinkVehiclesCounter(LinkVehiclesCounter2 linkVehiclesCounter)
	{
		this.linkVehiclesCounter = linkVehiclesCounter;
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
	
	public void setNetwork(Network network)
	{
		this.network = network;
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
		
		for (Link link : network.getLinks().values())
		{
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
				Map<Id, Integer> links2Update = linkVehiclesCounter.getChangedLinkVehiclesCounts();
			
				for (Id id : links2Update.keySet())
				{
					Link link = this.network.getLinks().get(id);
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
//		TravelTime travelTimeCalculatorClone;
//		if(this.travelTimeCalculator instanceof KnowledgeTravelTime)
//		{
//			travelTimeCalculatorClone = ((KnowledgeTravelTime)this.travelTimeCalculator).clone();
//		}
//		else
//		{
//			log.error("Could not clone the TimeCalculator - use reference to the existing Calculator and hope the best...");
//			travelTimeCalculatorClone = this.travelTimeCalculator;
//		}
		
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
		clone.useLookupTable = this.useLookupTable;
		clone.linkTravelTimes = this.linkTravelTimes;
		clone.setNetwork(this.network);
		clone.checkNodeKnowledge = this.checkNodeKnowledge;
		
		return clone;
	}
}
