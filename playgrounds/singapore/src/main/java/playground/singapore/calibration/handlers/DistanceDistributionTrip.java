package playground.singapore.calibration.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

/**
 * 
 * @author sergioo
 *
 */
public class DistanceDistributionTrip {

	//Private classes
	private class TravellerChain {
		
		//Attributes
		private List<String> modes = new ArrayList<String>();
		private List<Double> distances = new ArrayList<Double>();
		
	}
	
	//Attributes
	private Map<Id<Person>, TravellerChain> chains = new HashMap<Id<Person>, DistanceDistributionTrip.TravellerChain>();
	private Population population;
	private Network network;
	private TransitSchedule transitSchedule;
	private Set<Id<Person>> pIdsToExclude;

	//Constructors
	public DistanceDistributionTrip(Population population, Network network, TransitSchedule transitSchedule, Set<Id<Person>> pIdsToExclude) {
		this.network = network;
		this.transitSchedule = transitSchedule;
		this.population = population;
		this.pIdsToExclude = pIdsToExclude;
	}

	//Methods
	public void saveChains() {
		ExperimentalTransitRouteFactory factory =  new ExperimentalTransitRouteFactory();
		for(Person person:population.getPersons().values()) {
			if (pIdsToExclude.contains(person.getId())) { continue; }
			TravellerChain chain = new TravellerChain();
			boolean inPTLeg = false;
			boolean onlyWalk = false;
			double distancePT = 0;
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
				if(planElement instanceof Leg) {
					Leg leg = (Leg)planElement;
					double lastLinkLenght = network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();
					if(leg.getMode().equals("car")) {
						chain.distances.add(RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute)leg.getRoute(), network)+lastLinkLenght);
						chain.modes.add("car");
					}
					else if(leg.getMode().equals("transit_walk") && !inPTLeg) {
						inPTLeg = true;
						onlyWalk = true;
					}
					else if(leg.getMode().equals("pt") && inPTLeg)
						onlyWalk = false;
					if(leg.getMode().equals("pt")) {
						ExperimentalTransitRoute eRoute = (ExperimentalTransitRoute) factory.createRoute(leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());
						eRoute.setStartLinkId(leg.getRoute().getStartLinkId());
						eRoute.setEndLinkId(leg.getRoute().getEndLinkId());
						eRoute.setRouteDescription(leg.getRoute().getRouteDescription());
						TransitRoute route = transitSchedule.getTransitLines().get(eRoute.getLineId()).getRoutes().get(eRoute.getRouteId());
						distancePT += RouteUtils.calcDistanceExcludingStartEndLink(route.getRoute().getSubRoute(eRoute.getStartLinkId(), eRoute.getEndLinkId()), network)+lastLinkLenght;
					}
					else if(leg.getMode().equals("transit_walk"))
						distancePT += CoordUtils.calcEuclideanDistance(network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord(), network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord());
				}
				else if(planElement instanceof Activity)
					if(!((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) && inPTLeg) {
						chain.distances.add(distancePT);
						if(onlyWalk) {
							chain.modes.add("walk");
							onlyWalk = false;
						}
						else
							chain.modes.add("pt");
						inPTLeg = false;
						distancePT = 0;
					}
			chains.put(person.getId(), chain);
		}
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
			for(int i=0; i<chain.distances.size(); i++)
				distribution.get(getBin(distribution, chain.distances.get(i)))[getModeIndex(modes, chain.modes.get(i))]++;
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
		throw new RuntimeException("Mode "+mode+" in chains not specified in the distribution");
	}
	public void printDistribution(SortedMap<Integer, Integer[]> distribution, String distributionFile) throws FileNotFoundException {
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
	 * 1 - Transit schedule file
	 * 2 - Last iteration
	 * 3 - Iterations interval with events
	 * 4 - Output folder
	 * 5 - Distance bins file
	 * 6 - Distribution result folder
	 * @throws java.io.IOException
	 */
	public static void main(String[] args) throws IOException {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new MatsimNetworkReader(scenario.getNetwork()).parse(args[0]);
		new TransitScheduleReader(scenario).readFile(args[1]);
		int lastIteration = new Integer(args[2]);
		int iterationsInterval = new Integer(args[3]);
		for(int i=0; i<=lastIteration; i+=iterationsInterval) {
            scenario.setPopulation(PopulationUtils.createPopulation(scenario.getConfig(), scenario.getNetwork()));
			new MatsimPopulationReader(scenario).readFile(args[4]+"/ITERS/it."+i+"/"+i+".plans.xml.gz");
			DistanceDistributionTrip distanceDistribution = new DistanceDistributionTrip(scenario.getPopulation(), scenario.getNetwork(), scenario.getTransitSchedule(), new HashSet<Id<Person>>());
			distanceDistribution.saveChains();
			distanceDistribution.printDistribution(distanceDistribution.getDistribution(new String[]{args[5]}, new String[]{"car","pt","walk"}), args[6]+"/distanceDistribution2."+i+".csv");
		}
	}

}
