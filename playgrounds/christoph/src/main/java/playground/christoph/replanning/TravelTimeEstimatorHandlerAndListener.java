package playground.christoph.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.interfaces.CapacityInformationNetwork;
import org.matsim.core.population.routes.NetworkRoute;

import playground.christoph.events.ExtendedAgentReplanEventImpl;
import playground.christoph.events.handler.ExtendedAgentReplanEventHandler;
import playground.christoph.network.MyLinkImpl;
import playground.christoph.network.SubLink;

/*
 * The VehicleCounts are updated after all Replanning Actions within a 
 * simulated TimeStep, so all Agents plan their Routes based on the same
 * counts.
 * 
 * Each Agent creates a Replanning Event that contains the replanned Leg.
 * This Leg contains the new Route that is used to update the Vehicle Counts.
 * Doing this is an additional effort (basically calculating the travel time
 * a second time to identify the TimeSlots), but at the moment its is the
 * option with the smallest changes in the other parts of the Framework.
 * 
 * Updating the Counts only once per SimStep could leed to Problems if to many 
 * Agents plan their Routes in the same TimeStep (for example an Evacuation 
 * Scenario). One possible Solution would be an iterative Approach within the
 * TimeStep like "replan 10k Agents, update Counts, replan another 10k Agents, ...".    
 */
public class TravelTimeEstimatorHandlerAndListener implements 
		AgentArrivalEventHandler, AgentDepartureEventHandler,
		ExtendedAgentReplanEventHandler, AgentStuckEventHandler,
		LinkLeaveEventHandler,
		// IterationStartsListener,
		SimulationInitializedListener,
		SimulationBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(TravelTimeEstimatorHandlerAndListener.class);

	private Population population;
	private Network network;
	private CapacityInformationNetwork queueNetwork;
	private int travelTimeBinSize;
	private int numSlots;
	private TravelTimeEstimator travelTimeEstimator;
	private Map<Id, Integer> personLegCounter = new HashMap<Id, Integer>();

	private boolean useMyLinkImpls = true;
	/*
	 * We could instead use MyLinkImpls and add the Array to each single Link.
	 * Doing this would avoid the lookups in the HashMap. Use floats because
	 * they should use only half the memory size that doubles would need.
	 */
	private Map<Id, float[]> linkVehicleCounts; // LinkId
	private Map<Id, int[]> linkEnterCounts;	//	LinkId
	private Map<Id, int[]> linkLeaveCounts;	//	LinkId
	
	/*
	 * This List contains all Legs that are currently planned.
	 */
	private Map<Route, RouteInfo> routeInfos;

	/*
	 * If possible, we replace Routes directly instead of adding all new and then
	 * removing all old Routes. Doing so keeps the Network Load more or less stable.
	 * Otherwise the last Route that is added would see a totally different Load
	 * than the first one.
	 * Using TreeMaps and PersonIds ensures that the results keep deterministic.
	 */
	private Map<Id, ReplaceRouteContainer> routesToReplace;
	private Map<Id, NetworkRoute> routesToUpdate;
	private Map<Id, NetworkRoute> routesToAdd;
	private List<NetworkRoute> routesToRemove;

	private Map<Id, NetworkRoute> activeRoutes; // PersonId

	public TravelTimeEstimatorHandlerAndListener(Population population, Network network, int timeslice, int maxTime)
	{
		this.population = population;
		this.network = network;
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		
		init();
	}

	private void init()
	{
		travelTimeEstimator = new TravelTimeEstimator(this);
		
		routesToReplace = new TreeMap<Id, ReplaceRouteContainer>();	// PersonId
		routesToUpdate = new TreeMap<Id, NetworkRoute>();	// PersonId
		routesToAdd = new TreeMap<Id, NetworkRoute>();	// PersonId
		routesToRemove = new ArrayList<NetworkRoute>();

		activeRoutes = new HashMap<Id, NetworkRoute>();
		routeInfos = new HashMap<Route, RouteInfo>();
		
		// If we use MyLinksImpls the counts are stored directly in the Links
		if (useMyLinkImpls)
		{
			for (Link link : network.getLinks().values())
			{
				MyLinkImpl myLink = (MyLinkImpl) link;
				
				myLink.setLinkVehicleCounts(new float[this.numSlots]);
				myLink.setLinkEnterCounts( new int[this.numSlots]);
				myLink.setLinkLeaveCounts( new int[this.numSlots]);
			}
		}
		else
		{
			linkVehicleCounts = new HashMap<Id, float[]>();
			linkEnterCounts = new HashMap<Id, int[]>();
			linkLeaveCounts = new HashMap<Id, int[]>();

			for (Link link : network.getLinks().values())
			{
				linkVehicleCounts.put(link.getId(), new float[this.numSlots]);
				linkEnterCounts.put(link.getId(), new int[this.numSlots]);
				linkLeaveCounts.put(link.getId(), new int[this.numSlots]);
			}
		}
	}

	protected int getTravelTimeBinSize()
	{
		return this.travelTimeBinSize;
	}
	
	protected int getNumSlots()
	{
		return this.numSlots;
	}
	
	protected CapacityInformationNetwork getQueueNetwork()
	{
		return this.queueNetwork;
	}
	
	/*
	 * implements IterationStartsListener
	 * 
	 * Fills the Count Arrays with the initial Plans of the Agents.
	 * 
	 * [TODO] Should be done iteratively... Something like
	 * "balanceTravelTimes()" could help. This could be additionally done every
	 * n-TimeSteps to update the Arrays.
	 */
	// public void notifyIterationStarts(IterationStartsEvent event)
	public void notifySimulationInitialized(SimulationInitializedEvent event)
	{
		Simulation sim = event.getQueueSimulation();
		if (sim instanceof QueueSimulation)
		{
			queueNetwork = ((QueueSimulation) sim).getQueueNetwork();
		}
		else
		{
			log.error("Could not retrieve QueueNetwork from Simulation Object!");
		}
		
		// Initialize the Maps.
		init();

		// add initial Plans
		for (Person person : this.population.getPersons().values())
		{
			Plan plan = person.getSelectedPlan();

			for (PlanElement planElement : plan.getPlanElements())
			{
				if (planElement instanceof Leg)
				{
					Leg leg = (Leg) planElement;
					
					addRoute((NetworkRoute) leg.getRoute(), leg.getDepartureTime(), person.getId());
				}
			}
		}

		// checkLinkCounts();
	}

	private void checkLinkCounts()
	{
		float sum = 0.0f;
		for (float[] array : linkVehicleCounts.values())
		{
			for (float value : array)
			{
				sum = sum + value;
				if (value < 0)
					log.error(value);
			}
		}
		log.info(sum);

		for (Route route : routeInfos.keySet())
		{
			this.removeRoute(route);
		}

		sum = 0.0f;
		for (float[] array : linkVehicleCounts.values())
		{
			for (float value : array)
			{
				sum = sum + value;
				if (value < 0) log.error(value);
			}
		}
		log.info(sum);
	}

	/*
	 * Mind the order of the Actions. They influence the load of the Network.
	 * - First replace replanned Routes.
	 * - Then update Routes
	 * - Then remove old Routes
	 * - Finally add new Routes
	 */
	// implements QueueSimulationBeforeSimStepHandler
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent event)
	{	
		Iterator<NetworkRoute> iter;
		Iterator<ReplaceRouteContainer> iter2;
		Iterator<Entry<Id, NetworkRoute>> iter3;
				
		iter2 = this.routesToReplace.values().iterator();
		while (iter2.hasNext())
		{	
			ReplaceRouteContainer container = iter2.next();
			NetworkRoute originalRoute = container.oldRoute;
			NetworkRoute replannedRoute = container.newRoute;
			boolean removedRoute = this.removeRoute(originalRoute);
//			if (!removedRoute) log.error("Could not remove Route!");
			this.addRoute(replannedRoute, event.getSimulationTime(), container.personId);
			iter2.remove();
		}

		iter = this.routesToUpdate.values().iterator();
		while (iter.hasNext())
		{
			Route route = iter.next();
			updateRoute(route, event.getSimulationTime());
			iter.remove();
		}
		
		iter = this.routesToRemove.iterator();
		while (iter.hasNext())
		{
			NetworkRoute originalRoute = iter.next();
			boolean removedRoute = this.removeRoute(originalRoute);
			if (!removedRoute) log.error("Could not remove Route!");
			iter.remove();
		}
		
		iter3 = this.routesToAdd.entrySet().iterator();
		while (iter3.hasNext())
		{
			Entry<Id, NetworkRoute> entry = iter3.next();
			Id id = entry.getKey();
			NetworkRoute replannedRoute = entry.getValue();
			this.addRoute(replannedRoute, event.getSimulationTime(), id);
			iter3.remove();
		}
		
		this.checkCount(event.getSimulationTime());
	}

	private void checkCount(double time)
	{
		// Checking Parameters
		if (((int) time) % 1800 == 0)
		{
			float sum1 = 0.0f;
			if (this.useMyLinkImpls)
			{
				for (Link link : network.getLinks().values())
				{
					MyLinkImpl myLink = (MyLinkImpl)link;
					int index = this.getTimeSlotIndex(time);
					float[] array = myLink.getLinkVehicleCounts(); 
					sum1 = sum1 + array[index];
					if (array[index] < -0.01) log.error(array[index]);
				}	
			}
			else
			{
				for (float[] array : linkVehicleCounts.values())
				{
					int index = this.getTimeSlotIndex(time);
					sum1 = sum1 + array[index];
					if (array[index] < -0.01) log.error(array[index]);
				}				
			}

			float sum2 = 0.0f;
			for (Link link : network.getLinks().values())
			{
				// now we have MyLinkImpls that have a VehiclesCount variable :)
				int veh;

				// Do we use SubNetworks?
				if (link instanceof SubLink)
				{
					Link parentLink = ((SubLink) link).getParentLink();
					veh = ((MyLinkImpl) parentLink).getVehiclesCount();
				} 
				else
				{
					veh = ((MyLinkImpl) link).getVehiclesCount();
				}

				sum2 = sum2 + veh;
			}
			log.info("Expected Count: " + sum1 + ", Real Count:" + sum2 + ", Difference:" + (sum1 - sum2));
//			log.info("active Vehicles: " + this.activeRoutes.size());
		}
		// /Checking Parameters
	}
	
	/*
	 * If an Agent is removed from the Simulation we also have to remove its
	 * Route Data!
	 */
	// implements AgentStuckEventHandler
	public synchronized void handleEvent(AgentStuckEvent event)
	{
		Id personId = event.getPersonId();
		Person person = this.population.getPersons().get(personId);
		Plan plan = person.getSelectedPlan();

		for (PlanElement planElement : plan.getPlanElements())
		{
			if (planElement instanceof Leg)
			{
				Leg leg = (Leg) planElement;
				this.routesToRemove.add((NetworkRoute) leg.getRoute());
			}
		}
		
		Route activeRoute = this.activeRoutes.remove(event.getPersonId());
//		if (activeRoute != null) this.routesToUpdate.remove(activeRoute);
		if (activeRoute != null) this.routesToUpdate.remove(event.getPersonId());
	}

	// implements AgentArrivalEventHandler
	public synchronized void handleEvent(AgentArrivalEvent event)
	{
		Route activeRoute = this.activeRoutes.remove(event.getPersonId());
//		if (activeRoute != null) this.routesToUpdate.remove(activeRoute);		
		if (activeRoute != null) this.routesToUpdate.remove(event.getPersonId());
	}

	// implements AgentDepartureEventHandler
	public synchronized void handleEvent(AgentDepartureEvent event)
	{
		Leg leg = getNextPersonLeg(event.getPersonId());
		if (leg != null)
		{
			this.activeRoutes.put(event.getPersonId(), (NetworkRoute)leg.getRoute());
		}
		else
		{
			log.error("Could not retrieve Leg from AgentArrivalEvent!");
		}
	}

	private Leg getNextPersonLeg(Id personId) {
		Integer i = this.personLegCounter.get(personId);
		int ii;
		if (i == null) {
			ii = 1;
		} else {
			ii = i.intValue() + 1;
		}
		this.personLegCounter.put(personId, Integer.valueOf(ii));
		Person p = this.population.getPersons().get(personId);
		int cnt = 1;
		for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				if (cnt == ii) {
					return (Leg) pe;
				}
				cnt++;
			}
		}
		return null;
	}
	
	// implements LinkLeaveEventHandler
	public synchronized void handleEvent(LinkLeaveEvent event)
	{
		Id personId = event.getPersonId();

		NetworkRoute route = this.activeRoutes.get(personId);
		if (route != null)
		{
//			routesToUpdate.add(route);
			routesToUpdate.put(personId, route);
		} 
		else
		{
			log.error("Could not find active Route!");
		}
	}

	// implements ExtendedAgentReplanEventHandler
	public synchronized void handleEvent(ExtendedAgentReplanEventImpl event)
	{		
		NetworkRoute originalRoute = event.getOriginalRoute();
//		this.routesToRemove.add(originalRoute);

		NetworkRoute replannedRoute = event.getReplannedRoute();
//		this.routesToAdd.add(replannedRoute);

		ReplaceRouteContainer routesToReplace = new ReplaceRouteContainer(originalRoute, replannedRoute, event.getPersonId());
//		this.routesToReplace.add(routesToReplace);
		this.routesToReplace.put(event.getPersonId(), routesToReplace);
		
		// if the Route was active -> set new Route active
		if (this.activeRoutes.remove(event.getPersonId()) != null)
		{
			this.activeRoutes.put(event.getPersonId(), replannedRoute);
		}

		// if the Route should have been updated -> remove it from List
//		this.routesToUpdate.remove(originalRoute);
		this.routesToUpdate.remove(event.getPersonId());
	}

	// implements AgentArrivalEventHandler, AgentReplanEventHandler, ...
	public void reset(int iteration)
	{
		// TODO Auto-generated method stub
	}

	/*
	 * Implement this via a new Event? ReplanningEvent?
	 */
	private void addRoute(NetworkRoute route, double startTime, Id personId)
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

			this.addVehiclesToCount(linkId, enterTime, leaveTime);
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

	/*
	 * A Vehicle has just left a Link and enters a new one. So we can update its
	 * TravelTimes.
	 */
	private boolean updateRoute(Route route, double time)
	{
		RouteInfo routeInfo = routeInfos.get(route);

		if (routeInfo == null) return false;

		/*
		 * We update the Route because another Links has been processed. -> We
		 * have to update the Counter.
		 */
		int processedLinks = routeInfo.processedLinks;

		// get current Times and remove Vehicles from Count
		for (int i = processedLinks; i < routeInfo.linkIds.length; i++)
		{
			Id linkId = routeInfo.linkIds[i];
			double enterTime = routeInfo.times[i];
			double leaveTime = routeInfo.times[i + 1];

			this.removeVehiclesFromCount(linkId, enterTime, leaveTime);
		}

		// update the Times in the RouteInfo Object
		routeInfo.updateTimes(time);

		// add Vehicles with updated Times to the Count
		for (int i = processedLinks; i < routeInfo.linkIds.length; i++)
		{
			Id linkId = routeInfo.linkIds[i];
			double enterTime = routeInfo.times[i];
			double leaveTime = routeInfo.times[i + 1];

			this.addVehiclesToCount(linkId, enterTime, leaveTime);
		}

		routeInfo.processedLinks++;
		
		return true;
	}

	private int getTimeSlotIndex(double time)
	{
		int slice = ((int) time) / this.travelTimeBinSize;
		if (slice >= this.numSlots)
			slice = this.numSlots - 1;
		return slice;
	}

	private double getLowerTimeSlotBorder(int index)
	{
		return this.travelTimeBinSize * index;
	}

	private double getUpperTimeSlotBorder(int index)
	{
		return this.travelTimeBinSize * (index + 1);
	}

	/*
	 * The TimeSlot where a Vehicles enters a Link is included, the TimeSlot
	 * where the Vehicle leaves the Link is excluded. As a result trips that are
	 * shorter than a TimeSlot could be ignored if their total duration is
	 * within one TimeSlot. Otherwise the counts on Links with lots of ending
	 * Trips would become overestimated.
	 * 
	 * TimeSlots: |----|----|----|----|----|----|
	 * 
	 * Example: e ... enters Link l ... leaves Link Link 1:
	 * |-e--|----|--l-|----|----|----| Link 2: |-e--|----|--e-|----|--l-|----|
	 * 
	 * TimeSlots: |1111|1111|2222|2222|----|----|
	 */
	private void addVehiclesToCount(Id linkId, double enterTime, double leaveTime)
	{
		float[] vehicleCounts;
		int[] enterCounts;
		int[] leaveCounts;

		if (this.useMyLinkImpls)
		{
			MyLinkImpl myLink = (MyLinkImpl)this.network.getLinks().get(linkId);
			vehicleCounts = myLink.getLinkVehicleCounts();
			enterCounts = myLink.getLinkEnterCounts();
			leaveCounts = myLink.getLinkLeaveCounts();
		}
		else
		{
			vehicleCounts = linkVehicleCounts.get(linkId);
			enterCounts = linkEnterCounts.get(linkId);
			leaveCounts = linkLeaveCounts.get(linkId);			
		}
	
		int enterIndex = this.getTimeSlotIndex(enterTime);
		int leaveIndex = this.getTimeSlotIndex(leaveTime);

		double firstSlotUpperBoarder = this.getUpperTimeSlotBorder(enterIndex);
		double lastSlotLowerBoarder = this.getLowerTimeSlotBorder(leaveIndex);

		// We enter and leave the Link within one Slot
		if (enterIndex == leaveIndex)
		{
			vehicleCounts[enterIndex] = vehicleCounts[enterIndex] + (float) ((leaveTime - enterTime) / this.travelTimeBinSize);
			enterCounts[enterIndex] = enterCounts[enterIndex] + 1;
			leaveCounts[leaveIndex] = leaveCounts[leaveIndex] + 1;
		} 
		else
		{
			// first Slot
			vehicleCounts[enterIndex] = vehicleCounts[enterIndex] + (float) ((firstSlotUpperBoarder - enterTime) / this.travelTimeBinSize);
			enterCounts[enterIndex] = enterCounts[enterIndex] + 1;
			
			// full Slots between enter and leave Time
			int index = enterIndex + 1;
			while (index < leaveIndex)
			{
				vehicleCounts[index] = vehicleCounts[index] + 1.0f;
				index++;
			}

			// last Slot
			vehicleCounts[leaveIndex] = vehicleCounts[leaveIndex] + (float) ((leaveTime - lastSlotLowerBoarder) / this.travelTimeBinSize);
			leaveCounts[leaveIndex] = leaveCounts[leaveIndex] + 1;
		}
	}

	/*
	 * Have a look at the scheme that describes the addVehicleToCount method.
	 */
	private void removeVehiclesFromCount(Id linkId, double enterTime, double leaveTime)
	{
		float[] vehicleCounts;
		int[] enterCounts;
		int[] leaveCounts;

		if (this.useMyLinkImpls)
		{
			MyLinkImpl myLink = (MyLinkImpl)this.network.getLinks().get(linkId);
			vehicleCounts = myLink.getLinkVehicleCounts();
			enterCounts = myLink.getLinkEnterCounts();
			leaveCounts = myLink.getLinkLeaveCounts();
		}
		else
		{
			vehicleCounts = linkVehicleCounts.get(linkId);
			enterCounts = linkEnterCounts.get(linkId);
			leaveCounts = linkLeaveCounts.get(linkId);			
		}
		
		int enterIndex = this.getTimeSlotIndex(enterTime);
		int leaveIndex = this.getTimeSlotIndex(leaveTime);

		double firstSlotUpperBoarder = this.getUpperTimeSlotBorder(enterIndex);
		double lastSlotLowerBoarder = this.getLowerTimeSlotBorder(leaveIndex);

		// We enter and leave the Link within one Slot
		if (enterIndex == leaveIndex)
		{
			vehicleCounts[enterIndex] = vehicleCounts[enterIndex] - (float) ((leaveTime - enterTime) / this.travelTimeBinSize);
			enterCounts[enterIndex] = enterCounts[enterIndex] - 1;
			leaveCounts[leaveIndex] = leaveCounts[leaveIndex] - 1;
		}
		else
		{
			// first Slot
			vehicleCounts[enterIndex] = vehicleCounts[enterIndex] - (float) ((firstSlotUpperBoarder - enterTime) / this.travelTimeBinSize);
			enterCounts[enterIndex] = enterCounts[enterIndex] - 1;
			
			// full Slots between enter and leave Time
			int index = enterIndex + 1;
			while (index < leaveIndex)
			{
				vehicleCounts[index] = vehicleCounts[index] - 1.0f;
				index++;
			}

			// last Slot
			vehicleCounts[leaveIndex] = vehicleCounts[leaveIndex] - (float) ((leaveTime - lastSlotLowerBoarder) / this.travelTimeBinSize);
			leaveCounts[leaveIndex] = leaveCounts[leaveIndex] - 1;
		}
	}

	/*
	 * Contains all information about a Route that is currently used to
	 * calculate the number of vehicles per Link and TimeSlot. We store the
	 * LinkIds of the Route instead of the Route itself because the Route could
	 * be changed elsewhere.
	 */
	private class RouteInfo {

		// private NetworkRouteWRefs route;
		Id[] linkIds;
		int processedLinks = 0;
		// int[] timeSlotIndices;

		/*
		 * Contains the calculated Times where the Vehicles enters a Link. The
		 * last Entry contains the Arrival Time at the next Activity. A Vehicles
		 * is between times[i] and times[i+1] on the Link with linkIds[i].
		 */
		double[] times;

		public RouteInfo(NetworkRoute route)
		{
			// this.route = route;

			/*
			 * If the StartLink is != the EndLink. Otherwise
			 * the Route would connect two Activities that take place
			 * on the same Link. In that case the Vehicle will not reenter
			 * the Network when processing the Route.
			 * 
			 * We use the QueueSimulation logic (JDEQSim does the opposite):
			 * If an Agent is going to process an Activity on a Link
			 * then the QueueSimulation will let him driver over the 
			 * whole Link. After finishing the Activity the Agent is
			 * put to the end of the Link so he will immediately leave
			 * the Link.
			 */
			if (!route.getStartLinkId().equals(route.getEndLinkId()))
			{
				List<Id> list = route.getLinkIds();
				list.add(route.getEndLinkId());
				this.linkIds = new Id[list.size()];
				list.toArray(linkIds);
			}
			else
			{
				this.linkIds = new Id[0];
			}
		}

		private void calcTimes(double startTime)
		{
			double time = startTime;
			times = new double[linkIds.length + 1];
			times[0] = startTime;

			int i = 0;
			for (Id linkId : linkIds)
			{
				double travelTime = travelTimeEstimator.getLinkTravelTime(network.getLinks().get(linkId), time);

				time = time + travelTime;

				i++;
				times[i] = time;
			}
		}

		private void updateTimes(double currentTime)
		{
			double time = currentTime;
			times[processedLinks] = currentTime;

			for (int i = processedLinks; i < linkIds.length; i++)
			{
				Id linkId = linkIds[i];
				double travelTime = travelTimeEstimator.getLinkTravelTime(network.getLinks().get(linkId), time);

				time = time + travelTime;

				times[i + 1] = time;
			}
		}
	}
	
	private class ReplaceRouteContainer
	{
		Id personId;
		NetworkRoute oldRoute;
		NetworkRoute newRoute;
		
		public ReplaceRouteContainer(NetworkRoute oldRoute, NetworkRoute newRoute, Id personId)
		{
			this.oldRoute = oldRoute;
			this.newRoute = newRoute;
			this.personId = personId;
		}
	}

//	private class RouteComparator implements Comparator<NetworkRouteWRefs>
//	{
//		public int compare(NetworkRouteWRefs r1, NetworkRouteWRefs r2)
//		{
//			
//			
//			return r1.getVehicleId().compareTo(r2.getVehicleId());
//		}
//	}
//
//	private class ReplaceRouteContainerComparator implements Comparator<ReplaceRouteContainer>
//	{
//		public int compare(ReplaceRouteContainer r1, ReplaceRouteContainer r2)
//		{
//			return r1.oldRoute.getVehicleId().compareTo(r2.oldRoute.getVehicleId());
//		}	
//
//	}
}