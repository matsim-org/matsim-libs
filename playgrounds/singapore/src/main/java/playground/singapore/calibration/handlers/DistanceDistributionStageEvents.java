package playground.singapore.calibration.handlers;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * @author sergioo
 *
 */
public class DistanceDistributionStageEvents implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, TeleportationArrivalEventHandler, PersonStuckEventHandler {

	//Private classes
	private class PTVehicle {
	
		//Attributes
		boolean in = false;
		private Map<Id, Double> passengers = new HashMap<Id, Double>();
		private double distance;
		private Id transitLineId;
		private Id transitRouteId;
	
		//Constructors
		public PTVehicle(Id transitLineId, Id transitRouteId) {
			this.transitLineId = transitLineId;
			this.transitRouteId = transitRouteId;
		}
	
		//Methods
		public void incDistance(double linkDistance) {
			distance += linkDistance;
		}
		public void addPassenger(Id passengerId) {
			passengers.put(passengerId, distance);
		}
		public double removePassenger(Id passengerId) {
			return distance - passengers.remove(passengerId);
		}
	
	}
	private class TravellerChain {
		
		//Attributes
		boolean in = false;
		boolean traveledVehicle = false;
		private List<String> modes = new ArrayList<String>();
		private List<Double> distances = new ArrayList<Double>();
		
	}
	
	//Attributes
	private Map<Id, TravellerChain> chains = new HashMap<Id, DistanceDistributionStageEvents.TravellerChain>();
	private Map<Id, PTVehicle> ptVehicles = new HashMap<Id, DistanceDistributionStageEvents.PTVehicle>();
	private Map<Id, Coord> locations = new HashMap<Id, Coord>();
	private Network network;
	private TransitSchedule transitSchedule;
	private Set<Id> pIdsToExclude;
	private Map<Id, Integer> acts = new HashMap<Id, Integer>();
	private int stuck=0;

	//Constructors
	public DistanceDistributionStageEvents(Network network, TransitSchedule transitSchedule, Set<Id> pIdsToExclude) {
		this.network = network;
		this.transitSchedule = transitSchedule;
		this.pIdsToExclude = pIdsToExclude;
	}

	//Methods
	@Override
	public void reset(int iteration) {

	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		ptVehicles.put(event.getVehicleId(), new PTVehicle(event.getTransitLineId(), event.getTransitRouteId()));
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		if(!event.getPersonId().toString().startsWith("pt_tr"))
			if(event.getVehicleId().toString().startsWith("tr"))
				ptVehicles.get(event.getVehicleId()).addPassenger(event.getPersonId());
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		if(!event.getPersonId().toString().startsWith("pt_tr")) {
			TravellerChain chain = chains.get(event.getPersonId());
			if(event.getVehicleId().toString().startsWith("tr")) {
				chain.traveledVehicle = true;
				if(chain.modes.size()==chain.distances.size() && chain.modes.get(chain.modes.size()-1).equals("transit_walk")) {
					PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
					double stageDistance = vehicle.removePassenger(event.getPersonId());
					chain.distances.add(stageDistance);
					chain.modes.add(getMode(transitSchedule.getTransitLines().get(vehicle.transitLineId).getRoutes().get(vehicle.transitRouteId).getTransportMode(), vehicle.transitLineId));
				}
				else if(chain.modes.size()==chain.distances.size())
					throw new RuntimeException("Person is leaving a pt vehicle, but is not in a pt mode");
				else
					throw new RuntimeException("Modes list has not the same size of distances list (pt)");
			}
		}
	}
	private String getMode(String transportMode, Id line) {
		if(transportMode.contains("bus"))
			return "bus";
		else if(transportMode.contains("rail"))
			return "lrt";
		else if(transportMode.contains("subway"))
			if(line.toString().contains("PE") || line.toString().contains("SE") || line.toString().contains("SW"))
				return "lrt";
			else
				return "mrt";
		else
			return "other";
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (pIdsToExclude.contains(event.getDriverId())) { return; }
		if(event.getVehicleId().toString().startsWith("tr"))
			ptVehicles.get(event.getVehicleId()).in = true;
		else
			chains.get(event.getDriverId()).in = true;
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (pIdsToExclude.contains(event.getDriverId())) { return; }
		if(event.getVehicleId().toString().startsWith("tr")) {
			PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
			if(vehicle.in)
				vehicle.in = false;
			vehicle.incDistance(network.getLinks().get(event.getLinkId()).getLength());
		}
		else  {
			TravellerChain chain = chains.get(event.getDriverId());
			if(chain == null) {
				chain = new TravellerChain();
				chains.put(event.getDriverId(), chain);
				chain.modes.add("car");
				chain.distances.add(0.0);
			}
			else {
				if(chain.in) {
					chain.in = false;
					if(chain.modes.size()==chain.distances.size() && chain.modes.get(chain.modes.size()-1).equals("car")) {
						double linkDistance = network.getLinks().get(event.getLinkId()).getLength();
						chain.distances.set(chain.distances.size()-1, chain.distances.get(chain.distances.size()-1)+linkDistance);
					}
					else if(chain.modes.size()==chain.distances.size())
						throw new RuntimeException("Person is leaving a link, but is not in a car mode");
					else
						throw new RuntimeException("Modes list has not the same size of distances list (car)");
				}
				else {
					chain.modes.add("car");
					chain.distances.add(network.getLinks().get(event.getLinkId()).getLength());
				}
			}
		}
	}
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		TravellerChain chain = chains.get(event.getPersonId());
		if(!event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) && chain == null) {
			chain = new TravellerChain();
			chains.put(event.getPersonId(), chain);
			chain.distances.add(0.0);
			chain.modes.add("walk");
		}
		chain.in=false;
		if(event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
			chain.in = true;
		acts.put(event.getPersonId(), acts.get(event.getPersonId())==null?1:acts.get(event.getPersonId())+1);
		if(chain.modes.get(chain.modes.size()-1).equals("walk")) {
			Double distance = CoordUtils.calcEuclideanDistance(locations.get(event.getPersonId()), network.getLinks().get(event.getLinkId()).getCoord());
			if(chain.modes.size() == chain.distances.size()) {
				if(event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
					chain.modes.set(chain.modes.size()-1, "transit_walk");
				chain.distances.set(chain.distances.size()-1, distance);
			}
			else {
				chain.modes.set(chain.modes.size()-1, "transit_walk");
				chain.distances.add(distance);
			}
		}
	}
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		if(!event.getPersonId().toString().startsWith("pt_tr"))
			locations.put(event.getPersonId(), network.getLinks().get(event.getLinkId()).getCoord());
	}
	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		TravellerChain chain = chains.get(event.getPersonId());
		if(chain == null) {
			chain = new TravellerChain();
			chains.put(event.getPersonId(), chain);
		}
		if(chain.traveledVehicle)
			chain.traveledVehicle = false;
		else
			if(chain.in)
				chain.modes.add("walk");
			else {
				chain.modes.add("walk");
				chain.distances.add(0.0);
			}
	}
	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		if(!event.getPersonId().toString().startsWith("pt_tr")) {
			TravellerChain chain = chains.get(event.getPersonId());
			if(chain.distances.size() == chain.modes.size()) {
				chain.distances.remove(chain.distances.size()-1);
				chain.modes.remove(chain.modes.size()-1);
			}
			else
				throw new RuntimeException();
			stuck++;
		}
	}
	private SortedMap<Integer, Integer[]> getDistribution(String binsFile, String[] modes) throws IOException {
		SortedMap<Integer, Integer[]> distribution = new TreeMap<Integer, Integer[]>();
		BufferedReader reader = new BufferedReader(new FileReader(binsFile));
		String[] binTexts = reader.readLine().split(",");
		for(int i=0; i<binTexts.length; i++) {
			Integer[] numbers = new Integer[modes.length];
			for(int j=0; j<numbers.length; j++)
				numbers[j] = 0;
			distribution.put(new Integer(binTexts[i]), numbers);
		}
		for(TravellerChain chain:chains.values())
			if(chain.distances.size() == chain.modes.size())
				for(int i=0; i<chain.distances.size(); i++)
					distribution.get(getBin(distribution, chain.distances.get(i)))[getModeIndex(modes, chain.modes.get(i))]++;
			else
				throw new RuntimeException("Wrong chain");
		return distribution;
	}
	private Integer getBin(SortedMap<Integer, Integer[]> distribution, Double distance) {
		Integer bin = distribution.firstKey();
		for(Integer nextBin:distribution.keySet())
			if(distance<nextBin)
				return bin;
			else
				bin = nextBin;
		return bin;
	}
	private int getModeIndex(String[] modes, String mode) {
		for(int i=0; i<modes.length; i++)
			if(mode.equals(modes[i]))
				return i;
		throw new RuntimeException("Mode in chains not specified in the distribution");
	}
	private void printDistribution(SortedMap<Integer, Integer[]> distribution, String distributionFile) throws FileNotFoundException {
		PrintWriter printWriter = new PrintWriter(distributionFile);
		printWriter.println("distance,car,bus,mrt,lrt,transit_walk,walk,other");
		for(Entry<Integer, Integer[]> bin:distribution.entrySet()) {
			printWriter.print(bin.getKey());
			for(Integer number:bin.getValue())
				printWriter.print(","+number);
			printWriter.println();
		}
		printWriter.close();
	}

	//Main
	/**
	 * @param args
	 * 0 - Network file
	 * 1 - Transit schedule file
	 * 2 - Last iteration
	 * 3 - Iterations interval with events
	 * 4 - Output folder
	 * 5 - Distance bins file
	 * 6 - Distribution result folder
	 * @throws java.io.IOException
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new MatsimNetworkReader(scenario.getNetwork()).parse(args[0]);
		new TransitScheduleReader(scenario).readFile(args[1]);
		int lastIteration = new Integer(args[2]);
		int iterationsInterval = new Integer(args[3]);
		for(int i=0; i<=lastIteration; i+=iterationsInterval) {
			EventsManager eventsManager = EventsUtils.createEventsManager();
			DistanceDistributionStageEvents distanceDistribution = new DistanceDistributionStageEvents(scenario.getNetwork(), scenario.getTransitSchedule(), new HashSet<Id>());
			eventsManager.addHandler(distanceDistribution);
			new MatsimEventsReader(eventsManager).readFile(args[4]+"/ITERS/it."+i+"/"+i+".events.xml.gz");
			int tot=0;
			for(Integer act:distanceDistribution.acts.values())
				tot+=act;
			System.out.println(tot+" "+distanceDistribution.stuck);
			distanceDistribution.printDistribution(distanceDistribution.getDistribution(args[5], new String[]{"car","bus","mrt","lrt","transit_walk","walk","other"}), args[6]+"/distanceDistribution."+i+".csv");
		}
	}

}
