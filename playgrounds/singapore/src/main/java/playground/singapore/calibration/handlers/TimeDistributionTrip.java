package playground.singapore.calibration.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.pt.PtConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * @author sergioo
 *
 */
public class TimeDistributionTrip implements PersonDepartureEventHandler, ActivityStartEventHandler {

	//Private classes
	private class TravellerChain {
	
		//Attributes
		private double lastTime = 0;
		private boolean inPT = false;
		private boolean onlyWalk = false;
		private List<String> modes = new ArrayList<String>();
		private List<Double> times = new ArrayList<Double>();
	
	}

	//Attributes
	private Map<Id<Person>, TravellerChain> chains = new HashMap<Id<Person>, TimeDistributionTrip.TravellerChain>();
	private Set<Id<Person>> pIdsToExclude;

	//Constructor
	public TimeDistributionTrip(Set<Id<Person>> pIdsToExclude) {
		this.pIdsToExclude = pIdsToExclude;
	}

	//Methods
	@Override
	public void reset(int iteration) {
		chains.clear();
	}
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		TravellerChain chain = chains.get(event.getPersonId());
		if(event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) && !chain.inPT)
			chain.inPT = true;
		else if(!event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) && chain.inPT) {
			chain.modes.add("pt");
			chain.times.add(event.getTime()-chain.lastTime);
			chain.inPT = false;
		}
		else if(!event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			if(chain.onlyWalk)
				chain.modes.add("walk");
			else
				chain.modes.add("car");
			chain.times.add(event.getTime()-chain.lastTime);
		}
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (pIdsToExclude.contains(event.getPersonId())) { return; }
		TravellerChain chain = chains.get(event.getPersonId());
		if(chain == null) {
			chain = new TravellerChain();
			chains.put(event.getPersonId(), chain);
		}
		if(!chain.inPT) {
			chain.lastTime = event.getTime();
			if(event.getLegMode().equals("transit_walk"))
				chain.onlyWalk = true;
		}
		if(!event.getLegMode().equals("transit_walk"))
			chain.onlyWalk = false;
	}
	public SortedMap<Integer, Integer[]> getDistribution(String[] binTexts, String[] modes) throws IOException {
		SortedMap<Integer, Integer[]> distribution = new TreeMap<Integer, Integer[]>();
		for(int i=0; i<binTexts.length; i++) {
			Integer[] numbers = new Integer[modes.length];
			for(int j=0; j<numbers.length; j++)
				numbers[j] = 0;
			distribution.put(Math.round(new Float(binTexts[i])), numbers);
		}
		for(TravellerChain chain:chains.values())
			for(int i=0; i<chain.times.size(); i++)
				distribution.get(getBin(distribution, chain.times.get(i)))[getModeIndex(modes, chain.modes.get(i))]++;
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
	public void printDistribution(SortedMap<Integer, Integer[]> distribution, String distributionFile) throws FileNotFoundException {
		PrintWriter printWriter = new PrintWriter(distributionFile);
		printWriter.println("time,car,pt,walk");
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
	 * 0 - Last iteration
	 * 1 - Iterations interval with events
	 * 2 - Output folder
	 * 3 - Distance bins file
	 * 4 - Distribution result folder
	 * @throws java.io.IOException
	 */
	public static void main(String[] args) throws IOException {
		int lastIteration = new Integer(args[0]);
		int iterationsInterval = new Integer(args[1]);
		for(int i=0; i<=lastIteration; i+=iterationsInterval) {
			EventsManager eventsManager = EventsUtils.createEventsManager();
			TimeDistributionTrip timeDistribution = new TimeDistributionTrip(new HashSet<Id<Person>>());
			eventsManager.addHandler(timeDistribution);
			new MatsimEventsReader(eventsManager).readFile(args[2]+"/ITERS/it."+i+"/"+i+".events.xml.gz");
			timeDistribution.printDistribution(timeDistribution.getDistribution(new String[] {args[3]}, new String[]{"car","pt", "walk"}), args[4]+"/timeDistribution."+i+".csv");
		}
	}
		
}
