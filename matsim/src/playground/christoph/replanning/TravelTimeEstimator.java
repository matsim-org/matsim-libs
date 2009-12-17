package playground.christoph.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationBeforeSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.events.ExtendedAgentReplanEventImpl;
import playground.christoph.events.handler.ExtendedAgentReplanEventHandler;

/*
 * The VehicleCounts are updated after all Replanning Actions within a 
 * simulated TimeStep, so all Agents plan their Routes based on the same
 * counts.
 * 
 * Each Agent creates a Replanning Event that contains the replanned Leg.
 * This Leg contains the new Route that is used to update the Vehicle Counts.
 * Doing this is an additional effort (basically calculating the travel time
 * a second time to identify the TimeSlots), but at the moment its is the
 * option with the smalles changes in the other parts of the Framework.
 * 
 * Updating the Counts only once per SimStep could leed to Problems if to many 
 * Agents plan their Routes in the same TimeStep (for example an Evacuation 
 * Scenario). One possible Solution would be an iterative Approach within the
 * TimeStep like "replan 10k Agents, update Counts, replan another 10k Agents, ...".    
 */
public class TravelTimeEstimator implements TravelTime, 
	AgentArrivalEventHandler, AgentDepartureEventHandler, 
	ExtendedAgentReplanEventHandler,
//	IterationStartsListener,
	QueueSimulationInitializedListener, QueueSimulationBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(TravelTimeEstimator.class);
	
	private Population population;
	private Network network;
	private int travelTimeBinSize;
	private int numSlots;
	
	/*
	 * We could instead use MyLinkImpls and add the Array
	 * to each single Link. Doing this would avoid the
	 * lookups in the HashMap.
	 */
	private Map<Id, int[]> linkVehicleCounts;	// LinkId
	
	/*
	 * This List contains all Legs that are currently planned.
	 */
	private Map<Route, RouteInfo> routeInfos;
	
	private List<NetworkRouteWRefs> routesToAdd;
	private List<NetworkRouteWRefs> routesToRemove;
	
	public TravelTimeEstimator(Population population, Network network, int timeslice, int maxTime)
	{
		this.population = population;
		this.network = network;
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
	}
	
	private void init()
	{
		routesToAdd = new ArrayList<NetworkRouteWRefs>();
		routesToRemove = new ArrayList<NetworkRouteWRefs>();
		linkVehicleCounts = new HashMap<Id, int[]>();
		
		for (Link link : network.getLinks().values())
		{
			linkVehicleCounts.put(link.getId(), new int[this.numSlots]);
		}
		
		routeInfos = new HashMap<Route, RouteInfo>();
	}
	
	// implements TravelTime
	public synchronized double getLinkTravelTime(Link link, double time)
	{
		int timeSlotIndex = this.getTimeSlotIndex(time);

		return calcTravelTime(link, timeSlotIndex);
	}

	/*
	 * implements IterationStartsListener
	 * 
	 * Fills the Count Arrays with the initial Plans of the Agents.
	 * 
	 * [TODO] Should be done iteratively... 
	 * Something like "balanceTravelTimes()" could help. This could
	 * be additionally done every n-TimeSteps to update the Arrays.
	 */
//	public void notifyIterationStarts(IterationStartsEvent event)
	public void notifySimulationInitialized(QueueSimulationInitializedEvent e)
	{
		// Initialize the Maps.
		init();
		
		for (Person person : this.population.getPersons().values())
		{
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement planElement : plan.getPlanElements())
			{
				if (planElement instanceof Leg)
				{
					Leg leg = (Leg) planElement;
					RouteInfo routeInfo = new RouteInfo((NetworkRouteWRefs)leg.getRoute());
					routeInfo.calcTimes(leg.getDepartureTime());
					
					routeInfos.put(leg.getRoute(), routeInfo);
				}
			}
		}
	}
	
	// implements QueueSimulationBeforeSimStepHandler
	public void notifySimulationBeforeSimStep(QueueSimulationBeforeSimStepEvent e)
	{	
		Iterator<NetworkRouteWRefs> iter;
		
		iter = routesToAdd.iterator();
		while(iter.hasNext())
		{
			NetworkRouteWRefs replannedRoute = iter.next();
			addRoute(replannedRoute, e.getSimulationTime());
			iter.remove();
		}
		
		iter = routesToRemove.iterator();
		while(iter.hasNext())
		{
			NetworkRouteWRefs originalRoute = iter.next();
			boolean removeRoute = removeRoute(originalRoute);
			if (!removeRoute) log.error("Could not remove Route!");
			iter.remove();
		}
	}
	
	// implements AgentArrivalEventHandler
	public synchronized void handleEvent(AgentArrivalEvent event)
	{
//		if (event instanceof AgentEventImpl)
//		{
//			Leg leg = ((AgentEventImpl) event).getLeg();
//			
//			RouteInfo routeInfo = routeInfos.remove(leg);
//						
//			if (routeInfo != null)
//			{
//				this.removeRouteInfo(routeInfo);
//			}
//			else
//			{
//				log.warn("Could not find the Leg in the RouteInfos Map!");
//			}
//		}
//		else
//		{
//			log.error("Could not retrieve Leg from AgentArrivalEvent!");
//		}		
	}

	// implements AgentDepartureEventHandler
	public synchronized void handleEvent(AgentDepartureEvent event)
	{
		/*
		 * Do we need this?! We have all Legs already stored
		 */
	}
	
	// implements ExtendedAgentReplanEventHandler
	public synchronized void handleEvent(ExtendedAgentReplanEventImpl event)
	{
		NetworkRouteWRefs originalRoute = event.getOriginalRoute();
		this.routesToRemove.add(originalRoute);
		
		NetworkRouteWRefs replannedRoute = event.getReplannedRoute();
		this.routesToAdd.add(replannedRoute);
		
//		NetworkRouteWRefs originalRoute = event.getOriginalRoute();
//		boolean removeRoute = removeRoute(originalRoute);
//		if (!removeRoute)
//		{
//			log.error("Could not remove Route!" + routeInfos.size());
//		}
//		
//		NetworkRouteWRefs replannedRoute = event.getReplannedRoute();
//		addRoute(replannedRoute, event.getTime());
	}
	
	// implements AgentArrivalEventHandler, AgentReplanEventHandler
	public void reset(int iteration)
	{
		// TODO Auto-generated method stub		
	}
	
	/*
	 * Implement this via a new Event? ReplanningEvent?
	 */
	private void addRoute(NetworkRouteWRefs route, double startTime)
	{
		RouteInfo routeInfo = new RouteInfo(route);
		routeInfos.put(route, routeInfo);
		
		routeInfo.calcTimes(startTime);
		double[] times = routeInfo.times;
		
		int i = 0;
		for (Id linkId : routeInfo.linkIds)
		{	
			double enterTime = times[i];
			i++;
			double leaveTime = times[i];
			
			this.addVehicleToCount(linkId, enterTime, leaveTime);
		}
	}
	
	private boolean removeRoute(Route route)
	{
		RouteInfo routeInfo = routeInfos.get(route);
		
		if (routeInfo == null) return false;
		
		double[] times = routeInfo.times;
		
		int i = 0;
		for (Id linkId : routeInfo.linkIds)
		{	
			double enterTime = times[i];
			i++;
			double leaveTime = times[i];
			
			this.removeVehiclesFromCount(linkId, enterTime, leaveTime);
		}
		
		return true;
	}
	
	private int getTimeSlotIndex(double time)
	{
		int slice = ((int) time)/this.travelTimeBinSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}
	
	/*
	 * The TimeSlot where a Vehicles enters a Link is included,
	 * the TimeSlot where the Vehicle leaves the Link is excluded.
	 * As a result trips that are shorter than a TimeSlot could be
	 * ignored if their total duration is within one TimeSlot.
	 * Otherwise the counts on Links with lots of ending Trips would
	 * become overestimated. 
	 * 
	 * TimeSlots:
	 * |----|----|----|----|----|----|
	 * 
	 * Example:
	 * e ... enters Link
	 * l ... leaves Link
	 * Link 1: 		|-e--|----|--l-|----|----|----|
	 * Link 2: 		|-e--|----|--e-|----|--l-|----|
	 * 
	 * TimeSlots: 	|1111|1111|2222|2222|----|----|
	 * 
	 */
	private void addVehicleToCount(Id linkId, double enterTime, double leaveTime)
	{
		int[] linkCounts = linkVehicleCounts.get(linkId);
		
		int enterIndex = this.getTimeSlotIndex(enterTime);
		int leaveIndex = this.getTimeSlotIndex(leaveTime);
		
		int index = enterIndex;
		while (index < leaveIndex)
		{
			linkCounts[index] = linkCounts[index] + 1;
			index++;
		}
	}
	
	/*
	 * Have a look at the scheme that describes the addVehicleToCount method.
	 */
	private void removeVehiclesFromCount(Id linkId, double enterTime, double leaveTime)
	{
		int[] linkCounts = linkVehicleCounts.get(linkId);
		
		int enterIndex = this.getTimeSlotIndex(enterTime);
		int leaveIndex = this.getTimeSlotIndex(leaveTime);
		
		int index = enterIndex;
		while (index < leaveIndex)
		{
			linkCounts[index] = linkCounts[index] - 1;
			index++;
		}
	}
	
	private double calcTravelTime(Link link, int timeSlotIndex)
	{
		int[] array = linkVehicleCounts.get(link.getId());
		int vehiclesCount = array[timeSlotIndex];
		
		/*
		 * Calc TravelTime based on the vehiclesCount. We increase the count by
		 * one so we calculate the TravelTime that the Vehicles would have.
		 */
		vehiclesCount++;
		
		// calculate...

		
		return getLinkTravelTime(link, Time.UNDEFINED_TIME, vehiclesCount);
	}

	// calculate "real" travel time on the link and return it
	private double getLinkTravelTime(Link link, double time, double vehicles)
	{
		double tbuffer = 35.0;		// time distance ("safety distance") between two vehicles
		double vehicleLength = 7.5;	// length of a vehicle
		
		// if there are currently no vehicles on the link -> return the freespeed travel time
		if(vehicles == 0.0)
		{
			return link.getLength()/link.getFreespeed(time);
		}
		
		// normalize link to one lane
		vehicles = vehicles / link.getNumberOfLanes(time);
		
		double length = link.getLength();
		
		// At least one car can be on a link at a time.
		if (length < vehicleLength) length = vehicleLength;
		
		double vmax = link.getFreespeed(time);
		
		// velocity of a vehicle on the link
		double v = (length/vehicles - vehicleLength)/tbuffer;
		
		// Vehicles don't drive backwards.
		if (v < 0.0) v = 0.0;
		
		// limit the velocity if neccessary
		if(v > vmax) v = vmax;
		
		double travelTime;
		
		if (v > 0.0) travelTime = length / v;
		else travelTime = Double.MAX_VALUE;
		
		// check results
		double freespeedTravelTime = link.getLength()/link.getFreespeed(time);
		if(travelTime < freespeedTravelTime)
		{
			log.warn("TravelTime is shorter than FreeSpeedTravelTime - looks like something is wrong here. Using FreeSpeedTravelTime instead!");
			return freespeedTravelTime;
		}
		
		return travelTime;
	}
	
	/*
	 * Contains all information about a Route that is currently
	 * used to calculate the number of vehicles per Link and TimeSlot.
	 * We store the LinkIds of the Route instead of the Route itself
	 * because the Route could be changed elsewhere.
	 */
	private class RouteInfo{
	
//		private NetworkRouteWRefs route;
		Id[] linkIds;
//		int[] timeSlotIndices;
		
		/*
		 *  Contains the calculated Times where the Vehicles
		 *  enters a Link. The last Entry contains the Arrival Time
		 *  at the next Activity.
		 *  A Vehicles is between times[i] and times[i+1] on the Link
		 *  with linkIds[i]. 
		 */
		double[] times;
		
		public RouteInfo(NetworkRouteWRefs route)
		{
//			this.route = route;
			
			List<Id> list = route.getLinkIds();
			this.linkIds = new Id[list.size()]; 
			list.toArray(linkIds);
		}

		private void calcTimes(double startTime)
		{			
			double time = startTime;
			times = new double[linkIds.length + 1];
			times[0] = startTime;
			
			int i = 0;
			for (Id linkId : linkIds)
			{
				double travelTime = getLinkTravelTime(network.getLinks().get(linkId), time);
				
				time = time + travelTime;
			
				i++;
				times[i] = time;
			}
		}
	}
}