package playground.sergioo.calibration2012;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
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
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author sergioo
 *
 */
public class DistanceDistributionJourneyEvents implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, TeleportationArrivalEventHandler, PersonStuckEventHandler {

	//Private classes
	private class PTVehicle {
		
		//Attributes
		boolean in = false;
		private Map<Id<Person>, Double> passengers = new HashMap<Id<Person>, Double>();
		private double distance;
		
		//Methods
		public void incDistance(double linkDistance) {
			distance += linkDistance;
		}
		public void addPassenger(Id<Person> passengerId) {
			passengers.put(passengerId, distance);
		}
		public double removePassenger(Id<Person> passengerId) {
			return distance - passengers.remove(passengerId);
		}
		
	}
	private class TravellerChain {
		
		//Attributes
		boolean in = false;
		private List<String> modes = new ArrayList<String>();
		private List<Double> distances = new ArrayList<Double>();
		
	}
	
	//Attributes
	private Map<Id<Person>, TravellerChain> chains = new HashMap<Id<Person>, DistanceDistributionJourneyEvents.TravellerChain>();
	private Map<Id<Vehicle>, PTVehicle> ptVehicles = new HashMap<Id<Vehicle>, DistanceDistributionJourneyEvents.PTVehicle>();
	private Map<Id<Person>, Coord> locations = new HashMap<Id<Person>, Coord>();
	private Network network;
	private Map<Id<Person>, Integer> acts = new HashMap<Id<Person>, Integer>();
	private int stuck=0;
	
	//Constructors
	public DistanceDistributionJourneyEvents(Network network) {
		this.network = network;
	}
	
	//Methods
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		ptVehicles.put(event.getVehicleId(), new PTVehicle());
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(!event.getPersonId().toString().startsWith("pt_tr"))
			if(event.getVehicleId().toString().startsWith("tr"))
				ptVehicles.get(event.getVehicleId()).addPassenger(event.getPersonId());
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(!event.getPersonId().toString().startsWith("pt_tr")) {
			TravellerChain chain = chains.get(event.getPersonId());
			if(event.getVehicleId().toString().startsWith("tr")) {
				if(chain.modes.size()==chain.distances.size() && chain.modes.get(chain.modes.size()-1).equals("pt")) {
					double stageDistance = ptVehicles.get(event.getVehicleId()).removePassenger(event.getPersonId());
					chain.distances.set(chain.distances.size()-1, chain.distances.get(chain.distances.size()-1)+stageDistance);
					chain.in = true;
				}
				else if(chain.modes.size()==chain.distances.size())
					throw new RuntimeException("Person is leaving a pt vehicle, but is not in a pt mode");
				else
					throw new RuntimeException("Modes list has not the same size of distances list (pt)");
			}
		}
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getVehicleId().toString().startsWith("tr"))
			ptVehicles.get(event.getVehicleId()).in = true;
		else
			chains.get(event.getDriverId()).in = true;
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(event.getVehicleId().toString().startsWith("tr")) {
			PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
			if(vehicle.in) {
				vehicle.in = false;
				vehicle.incDistance(network.getLinks().get(event.getLinkId()).getLength());
			}
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
					chain.distances.add(0.0);
				}
			}
		}
	}
	@Override
	public void handleEvent(ActivityStartEvent event) {
		TravellerChain chain = chains.get(event.getPersonId());
		if(!event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			if(chain == null) {
				chain = new TravellerChain();
				chains.put(event.getPersonId(), chain);
				chain.distances.add(0.0);
				chain.modes.add("walk");
			}
			chains.get(event.getPersonId()).in=false;
			acts.put(event.getPersonId(), acts.get(event.getPersonId())==null?1:acts.get(event.getPersonId())+1);
		}
		if(chain.modes.get(chain.modes.size()-1).equals("walk")) {
			Double distance = CoordUtils.calcEuclideanDistance(locations.get(event.getPersonId()), network.getLinks().get(event.getLinkId()).getCoord());
			if(chain.modes.size() == chain.distances.size()) {
				if(event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
					chain.modes.set(chain.modes.size()-1, "pt");
				chain.distances.set(chain.distances.size()-1, distance);
			}
			else {
				chain.modes.remove(chain.modes.size()-1);
				chain.distances.set(chain.distances.size()-1, chain.distances.get(chain.distances.size()-1)+distance);
			}
		}
	}
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(!event.getPersonId().toString().startsWith("pt_tr"))
			locations.put(event.getPersonId(), network.getLinks().get(event.getLinkId()).getCoord());
	}
	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		TravellerChain chain = chains.get(event.getPersonId());
		if(chain == null) {
			chain = new TravellerChain();
			chains.put(event.getPersonId(), chain);
		}
		if(chain.in)
			chain.modes.add("walk");
		else {
			chain.modes.add("walk");
			chain.distances.add(0.0);
		}	
	}
	@Override
	public void handleEvent(PersonStuckEvent event) {
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
		reader.close();
		for(int i=0; i<binTexts.length; i++) {
			Integer[] numbers = new Integer[modes.length];
			for(int j=0; j<numbers.length; j++)
				numbers[j] = 0;
			distribution.put(new Integer(binTexts[i]), numbers);
		}
		for(TravellerChain chain:chains.values())
			for(int i=0; i<chain.distances.size(); i++)
				distribution.get(getBin(distribution, chain.distances.get(i)))[getModeIndex(modes, chain.modes.get(i))]++;
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
		printWriter.println("distance,car,pt,walk");
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
	 * 1 - Last iteration
	 * 2 - Iterations interval with events
	 * 3 - Output folder
	 * 4 - Distance bins file
	 * 5 - Distribution result folder
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).parse(args[0]);
		int lastIteration = new Integer(args[1]);
		int iterationsInterval = new Integer(args[2]);
		for(int i=0; i<=lastIteration; i+=iterationsInterval) {
			EventsManager eventsManager = EventsUtils.createEventsManager();
			DistanceDistributionJourneyEvents distanceDistribution = new DistanceDistributionJourneyEvents(scenario.getNetwork());
			eventsManager.addHandler(distanceDistribution);
			new MatsimEventsReader(eventsManager).readFile(args[3]+"/ITERS/it."+i+"/"+i+".events.xml.gz");
			int tot=0;
			for(Integer act:distanceDistribution.acts.values())
				tot+=act;
			System.out.println(tot+" "+distanceDistribution.stuck);
			distanceDistribution.printDistribution(distanceDistribution.getDistribution(args[4], new String[]{"car","pt", "walk"}), args[5]+"/distanceDistribution."+i+".csv");
		}
	}

}
