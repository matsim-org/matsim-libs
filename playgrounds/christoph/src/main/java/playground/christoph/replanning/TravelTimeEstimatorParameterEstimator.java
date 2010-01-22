package playground.christoph.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;

/*
 * Try to estimate the Paramters for a TravelTimeEstimator
 * by evaluating a Events File.
 * 
 * - Collect all Link Trips (LinkEnter, LinkLeave, Flows)
 */
public class TravelTimeEstimatorParameterEstimator implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentStuckEventHandler{

	private final Population population;
	private final Network network;
	private final int travelTimeBinSize;
	private final int numSlots;
	
	private Map<Id, float[]> linkVehicleCounts; // LinkId
	private Map<Id, int[]> linkEnterCounts;	//	LinkId
	private Map<Id, int[]> linkLeaveCounts;	//	LinkId
	
	private Map<Id, TripBin> activeTrips;	// PersonId
	private Map<Id, List<TripBin>> finishedTrips;	// LinkId
	
	private static String networkFile = "mysimulations/kt-zurich/input/network.xml";
	private static String populationFile = "mysimulations/kt-zurich/input/plans.xml.gz";
	private static String eventsFile = "mysimulations/kt-zurich/output/ITERS/it.0/0.events.txt.gz";
	
	public static void main(String[] args)
	{
		// Load Network
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		// Load Population
		new MatsimPopulationReader(scenario).readFile(populationFile);
				
		// Instance which takes over line by line of the events file
		// and throws events of added types
		EventsManagerImpl events = new EventsManagerImpl();
		
		// An example of an events handler which takes
		// "LinkLeaveEvents" to calculate total volumes per link of the network
		TravelTimeEstimatorParameterEstimator handler = new TravelTimeEstimatorParameterEstimator(scenario.getPopulation(), network, 600, 86400 * 2);
		
		// register the handler to the events object
		events.addHandler(handler);
		
		// Reader to read events line by line and parses it over to the events object
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(eventsFile);
		
		// an example output of the DailyLinkVolumeCalc
		System.out.println("LinkEnterEvents: " + handler.linkEnterCounts.size());
		System.out.println("LinkLeaveEvents: " + handler.linkLeaveCounts.size());
	}
	
	public TravelTimeEstimatorParameterEstimator(Population population, Network network, int timeslice, int maxTime)
	{
		this.population = population;
		this.network = network;
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		
		init();
	}

	private void init()
	{
		activeTrips = new HashMap<Id, TripBin>();
		
		finishedTrips = new HashMap<Id, List<TripBin>>();
		linkVehicleCounts = new HashMap<Id, float[]>();
		linkEnterCounts = new HashMap<Id, int[]>();
		linkLeaveCounts = new HashMap<Id, int[]>();

		for (Link link : network.getLinks().values())
		{
			finishedTrips.put(link.getId(), new ArrayList<TripBin>());
			linkVehicleCounts.put(link.getId(), new float[this.numSlots]);
			linkEnterCounts.put(link.getId(), new int[this.numSlots]);
			linkLeaveCounts.put(link.getId(), new int[this.numSlots]);
		}
	}
	
	private int getTimeSlotIndex(double time)
	{
		int slice = ((int) time) / this.travelTimeBinSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}
	
	public void reset(int iteration)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void handleEvent(LinkEnterEvent event)
	{
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		double time = event.getTime();
		
		int[] counts = linkEnterCounts.get(linkId);
		
		int timeSlot = this.getTimeSlotIndex(time);
		counts[timeSlot] = counts[timeSlot] + 1;
		
		TripBin tripBin = new TripBin();
		tripBin.enterTime = time;
		tripBin.personId = personId;
		tripBin.linkId = linkId;
		
		this.activeTrips.put(personId, tripBin);
	}

	public void handleEvent(LinkLeaveEvent event)
	{
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		double time = event.getTime();
		
		int[] counts = linkEnterCounts.get(linkId);
		
		int timeSlot = this.getTimeSlotIndex(time);
		counts[timeSlot] = counts[timeSlot] - 1;
		
		TripBin tripBin = this.activeTrips.remove(personId);
		if (tripBin != null)
		{
			tripBin.leaveTime = time;
			
			List<TripBin> list = this.finishedTrips.get(linkId);
			list.add(tripBin);
		}
	}

	public void handleEvent(AgentStuckEvent event)
	{
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		double time = event.getTime();
		
		int[] counts = linkEnterCounts.get(linkId);
		
		int timeSlot = this.getTimeSlotIndex(time);
		counts[timeSlot] = counts[timeSlot] - 1;
		
		this.activeTrips.remove(personId);
	}

	private class TripBin {
		
		Id personId;
		Id linkId;
		double enterTime;
		double leaveTime;
		int vehicleCount;
	}
}
