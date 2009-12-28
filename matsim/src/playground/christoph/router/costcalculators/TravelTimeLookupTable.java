package playground.christoph.router.costcalculators;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.events.LinkVehiclesCounter2;
import playground.christoph.router.util.LookupTable;

/*
 * This Class implements a LookupTable that can be
 *  used in Combination with any TravelTimeCalculator.
 *  
 *  It may result in a SpeedUp if the Calculation of
 *  the LinkTravelCosts are very time consuming.
 */
public class TravelTimeLookupTable implements TravelTime, LookupTable, Cloneable {
	
	private static final Logger log = Logger.getLogger(TravelTimeLookupTable.class);
	
	private TravelTime travelTimeCalculator;
	private boolean useLookupTable = true;
	private double lastUpdate = Time.UNDEFINED_TIME;
	protected Map<Id, Double> lookupTable;
	private Network network;
	private LinkVehiclesCounter2 linkVehiclesCounter;
	
	public TravelTimeLookupTable(TravelTime travelTime, Network network, LinkVehiclesCounter2 linkVehiclesCounter)
	{
		this.travelTimeCalculator = travelTime;
		this.network = network;
		this.linkVehiclesCounter = linkVehiclesCounter;
		this.lookupTable = new HashMap<Id, Double>(); 
	}
	
	public void useLookupTable(boolean value)
	{
		this.useLookupTable = value;
	}

	public boolean useLookupTable()
	{
		return this.useLookupTable;
	}
	
	public double getLinkTravelTime(Link link, double time)
	{
		if(useLookupTable)
		{
			return lookupTable.get(link.getId());
		}
		else 
		{
			return travelTimeCalculator.getLinkTravelTime(link, time);
		}
	}
	
	private void createLookupTable(double time)
	{
		resetLookupTable();
		
		for (Link link : network.getLinks().values())
		{
			lookupTable.put(link.getId(), travelTimeCalculator.getLinkTravelTime(link, time));
		}
	}
	
	public void updateLookupTable(double time)
	{
		if (useLookupTable)
		{
			if (lastUpdate != time)
			{
				// executed only initially
				if (lookupTable.size() == 0) createLookupTable(time);
								
				Map<Id, Integer> links2Update = linkVehiclesCounter.getChangedLinkVehiclesCounts();

				for (Id id : links2Update.keySet())
				{
					Link link = network.getLinks().get(id);
					lookupTable.put(id, travelTimeCalculator.getLinkTravelTime(link, time));
				}
				lastUpdate = time;
			}
		}
	}
	
	public void resetLookupTable()
	{
		lookupTable.clear();
	}
	
	public double lastUpdate()
	{
		return this.lastUpdate;
	}
	
	@Override
	public TravelTimeLookupTable clone()
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
		
		TravelTimeLookupTable clone = new TravelTimeLookupTable(travelTimeCalculatorClone, this.network, this.linkVehiclesCounter);
		clone.useLookupTable = this.useLookupTable;
		
		// We don't clone the LookupTable!
		clone.lookupTable = this.lookupTable;
		
		return clone;
	}
}
