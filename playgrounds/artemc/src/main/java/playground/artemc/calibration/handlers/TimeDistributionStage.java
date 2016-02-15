package playground.artemc.calibration.handlers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

/**
 * 
 * @author sergioo
 *
 */
public class TimeDistributionStage implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonDepartureEventHandler, ActivityStartEventHandler {

	//Private classes
	private class PTVehicle {
	
		//Attributes
		private Id lineId;
		private Id routeId;
	
		//Constructors
		public PTVehicle(Id transitLineId, Id transitRouteId) {
			this.lineId = transitLineId;
			this.routeId = transitRouteId;
		}
	
	}
	private class TravellerChain {
	
		//Attributes
		private double lastTime = 0;
		private boolean inPT = false;
		private boolean walk = false;
		private Id lineId;
		private Id routeId;
		private List<String> modes = new ArrayList<String>();
		private List<Double> times = new ArrayList<Double>();
	
	}

	//Attributes
	private Map<Id, TravellerChain> chains = new HashMap<Id, TimeDistributionStage.TravellerChain>();
	private Map<Id, PTVehicle> ptVehicles = new HashMap<Id, TimeDistributionStage.PTVehicle>();
	private TransitSchedule transitSchedule;
	private Set<Id> pIdsToExclude;

	
	public TimeDistributionStage(TransitSchedule transitSchedule, Set<Id> pIdsToExclude) {
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
			if(event.getVehicleId().toString().startsWith("tr")) {
				TravellerChain chain = chains.get(event.getPersonId());
				chain.lineId = ptVehicles.get(event.getVehicleId()).lineId;
				chain.routeId = ptVehicles.get(event.getVehicleId()).routeId;
			}
	}
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		TravellerChain chain = chains.get(event.getPersonId());
		boolean beforeInPT = chain.inPT;
		if(event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
			chain.inPT = true;
		else
			chain.inPT = false;
		if(!chain.inPT && !chain.walk)
			chain.modes.add("car");
		else if(!chain.inPT && chain.walk && !beforeInPT)
			chain.modes.add("walk");
		else if(chain.walk)
			chain.modes.add("transit_walk");
		else
			chain.modes.add(getMode(transitSchedule.getTransitLines().get(chain.lineId).getRoutes().get(chain.routeId).getTransportMode(), chain.lineId));
		chain.times.add(event.getTime()-chain.lastTime);
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		TravellerChain chain = chains.get(event.getPersonId());
		if(chain == null) {
			chain = new TravellerChain();
			chains.put(event.getPersonId(), chain);
		}
		chain.lastTime = event.getTime();
		if(event.getLegMode().equals("transit_walk"))
			chain.walk = true;
		else
			chain.walk = false;
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
			for(int i=0; i<chain.times.size(); i++)
				distribution.get(getBin(distribution, chain.times.get(i)))[getModeIndex(modes, chain.modes.get(i))]++;
		chains.clear();
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
	 * 0 - Transit schedule file
	 * 1 - Last iteration
	 * 2 - Iterations interval with events
	 * 3 - Output folder
	 * 4 - Distance bins file
	 * 5 - Distribution result folder
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		int lastIteration = new Integer(args[1]);
		int iterationsInterval = new Integer(args[2]);
		for(int i=0; i<=lastIteration; i+=iterationsInterval) {
			EventsManager eventsManager = EventsUtils.createEventsManager();
			TimeDistributionStage timeDistribution = new TimeDistributionStage(scenario.getTransitSchedule(), new HashSet<Id>());
			eventsManager.addHandler(timeDistribution);
			new MatsimEventsReader(eventsManager).readFile(args[3]+"/ITERS/it."+i+"/"+i+".events.xml.gz");
			timeDistribution.printDistribution(timeDistribution.getDistribution(args[4], new String[]{"car","bus","mrt","lrt","transit_walk","walk","other"}), args[5]+"/timeDistribution."+i+".csv");
		}
	}
		
}
