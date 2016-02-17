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
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

/**
 * 
 * @author sergioo
 *
 */
public class DistanceDistributionStage {

	//Private classes
	private class TravellerChain {
		
		//Attributes
		private List<String> modes = new ArrayList<String>();
		private List<Double> distances = new ArrayList<Double>();
		
	}
	
	//Attributes
	private Map<Id<Person>, TravellerChain> chains = new HashMap<Id<Person>, DistanceDistributionStage.TravellerChain>();
	private Population population;
	private Network network;
	private TransitSchedule transitSchedule;
	
	//Constructors
	public DistanceDistributionStage(Population population, Network network, TransitSchedule transitSchedule) {
		this.population = population;
		this.network = network;
		this.transitSchedule = transitSchedule;
	}
	
	//Methods
	private void saveChains() {
		ExperimentalTransitRouteFactory factory =  new ExperimentalTransitRouteFactory();
		for(Person person:population.getPersons().values()) {
			TravellerChain chain = new TravellerChain();
			List<PlanElement> elements = person.getSelectedPlan().getPlanElements();
			for(int i=0; i<elements.size(); i++) {
				PlanElement planElement = elements.get(i);
				if(planElement instanceof Leg) {
					Leg leg = (Leg)planElement;
					double lastLinkLenght = network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();
					if(leg.getMode().equals("car")) {
						chain.distances.add(lastLinkLenght+RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute)leg.getRoute(), network));
						chain.modes.add("car");
					}
					else if(leg.getMode().equals("transit_walk")) {
						chain.distances.add(CoordUtils.calcEuclideanDistance(network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord(), network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord()));
						if(((Activity)elements.get(i-1)).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)||((Activity)elements.get(i+1)).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
							chain.modes.add("transit_walk");
						else
							chain.modes.add("walk");
					}
					else if(leg.getMode().equals("pt")) {
						ExperimentalTransitRoute eRoute = (ExperimentalTransitRoute) factory.createRoute(leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());
						eRoute.setStartLinkId(leg.getRoute().getStartLinkId());
						eRoute.setEndLinkId(leg.getRoute().getEndLinkId());
						eRoute.setRouteDescription((leg.getRoute()).getRouteDescription());
						TransitLine line = transitSchedule.getTransitLines().get(eRoute.getLineId());
						chain.distances.add(lastLinkLenght+RouteUtils.calcDistanceExcludingStartEndLink(line.getRoutes().get(eRoute.getRouteId()).getRoute().getSubRoute(eRoute.getStartLinkId(), eRoute.getEndLinkId()), network));
						chain.modes.add(getMode(line.getRoutes().get(eRoute.getRouteId()).getTransportMode(), line.getId()));
					}
				}
			chains.put(person.getId(), chain);
			}
		}
	}
	private String getMode(String transportMode, Id<TransitLine> line) {
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
		reader.close();
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
	 * @throws IOException 
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
			DistanceDistributionStage distanceDistribution = new DistanceDistributionStage(scenario.getPopulation(), scenario.getNetwork(), scenario.getTransitSchedule());
			distanceDistribution.saveChains();
			distanceDistribution.printDistribution(distanceDistribution.getDistribution(args[5], new String[]{"car","bus","mrt","lrt","transit_walk","walk","other"}), args[6]+"/distanceDistribution2."+i+".csv");
		}
	}

}
