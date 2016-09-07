package saleem.p0.stockholm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.plot.XYPlot;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;

import saleem.stockholmscenario.utils.CollectionUtil;

public class NetworkAnalysis {
	public static void main(String[] args){
		NetworkAnalysis analyser = new NetworkAnalysis();
//		analyser.analysePlans();
		analyser.analyseEvents();
//		analyser.analysePlans();
	}
	public void analyseEvents(){
		List<String> allincominglinksids = new ArrayList<String>();//Ids of incoming links in String form
		String path = "./ihop2/matsim-input/config - P0.xml";
		final Config config = ConfigUtils.loadConfig(path);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = (Network)scenario.getNetwork();
		StockholmP0Helper sth = new StockholmP0Helper(network);
		String nodesfile = "./ihop2/matsim-input/Nodes.csv";
		List<String> timednodes = sth.getPretimedNodes(nodesfile);
		Map<String, List<Link>> inlinksforjunctions = sth.getInLinksForJunctions(timednodes, network);
		Iterator<String> nodes = inlinksforjunctions.keySet().iterator();//List of incoming links for each pretimed node
		while(nodes.hasNext()){
			String node = nodes.next();
			List<Link> inlinks = inlinksforjunctions.get(node);
			if(inlinks.size()<2){
				System.out.println(node);
			}
			else{
				Iterator<Link> linksiter = inlinks.iterator();
				while(linksiter.hasNext()){
					allincominglinksids.add(linksiter.next().getId().toString());
				}
			}
		}
//		final EventsManager eventsNoP0 = EventsUtils.createEventsManager(config);
//		EventsHandler handlerNoP0 = new EventsHandler(allincominglinksids, scenario.getNetwork());
//		eventsNoP0.addHandler(handlerNoP0);
//		//events.addHandler(handler);
//		final MatsimEventsReader readerNoP0 = new MatsimEventsReader(eventsNoP0);
//		readerNoP0.readFile("C:\\Results Matsim\\P0\\Gunnar PC\\NP01\\it.1000\\1000.events.xml.gz");
//		
		final EventsManager eventsPlainNoP0 = EventsUtils.createEventsManager(config);
		EventsHandler handlerPlainNoP0 = new EventsHandler(allincominglinksids, scenario.getNetwork());
		eventsPlainNoP0.addHandler(handlerPlainNoP0);
		//events.addHandler(handler);
		final MatsimEventsReader readerPlainNoP0 = new MatsimEventsReader(eventsPlainNoP0);
		readerPlainNoP0.readFile("C:\\Results Matsim\\matsim-output-statsandplots\\PNP04\\it.950\\950.events.xml.gz");
		
		final EventsManager eventsP0 = EventsUtils.createEventsManager(config);
		EventsHandler handlerP0 = new EventsHandler(allincominglinksids, scenario.getNetwork());
		eventsP0.addHandler(handlerP0);
		final MatsimEventsReader readerP0 = new MatsimEventsReader(eventsP0);
		readerP0.readFile("C:\\Results Matsim\\matsim-output-statsandplots\\P04\\it.950\\950.events.xml.gz");
		plotDelays("./ihop2/matsim-input/delays.png",handlerP0.getTimes(), handlerP0.getDelays(),handlerPlainNoP0.getDelays());
		plotTravellersThroughNodes("./ihop2/matsim-input/travellersthrooughnodes.png",handlerP0.getTimes(), handlerP0.getNumAgentsThroughIntersections(), handlerPlainNoP0.getNumAgentsThroughIntersections());
		plotTravellersOnIncomingLinks("./ihop2/matsim-input/travellersonincominglinks.png",handlerP0.getTimes(), handlerP0.getAgentsOnIncomingLinks(),handlerPlainNoP0.getAgentsOnIncomingLinks());
		System.out.println();

	}
	public void plotDelays(String path, List<Double> times, List<Double> delaysp0, List<Double> delaysplainnop0){
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		XYLineChart chart = new XYLineChart("Delay Statistics", "Time (Hour)", "Delay (Sec)");
		XYPlot plot = (XYPlot)chart.getChart().getPlot();
		chart.addSeries("P0 Applied", cutil.toArray(times), cutil.toArray(delaysp0));
//		chart.addSeries("P0 Not Applied", cutil.toArray(times), cutil.toArray(delaysnop0));
		chart.addSeries("Plain P0 Not Applied", cutil.toArray(times), cutil.toArray(delaysplainnop0));
		chart.saveAsPng(path, 800, 600);
	}
	public void plotTravellersThroughNodes(String path, List<Double> times, List<Double> nump0, List<Double> numplainnop0){
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		XYLineChart chart = new XYLineChart("Number of Travellers Through Pretimed Nodes", "Time (Hour)", "Number");
		XYPlot plot = (XYPlot)chart.getChart().getPlot();
		chart.addSeries("P0 Applied", cutil.toArray(times), cutil.toArray(nump0));
//		chart.addSeries("P0 Not Applied", cutil.toArray(times), cutil.toArray(numnop0));
		chart.addSeries("Plain P0 Not Applied", cutil.toArray(times), cutil.toArray(numplainnop0));
		chart.saveAsPng(path, 800, 600);
	}
	public void plotTravellersOnIncomingLinks(String path, List<Double> times, List<Double> nump0, List<Double> numplainnop0){
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		XYLineChart chart = new XYLineChart("Number of Travellers On Incoming Links", "Time (Hour)", "Number");
		XYPlot plot = (XYPlot)chart.getChart().getPlot();
		chart.addSeries("P0 Applied", cutil.toArray(times), cutil.toArray(nump0));
//		chart.addSeries("P0 Not Applied", cutil.toArray(times), cutil.toArray(numnop0));
		chart.addSeries("Plain P0 Not Applied", cutil.toArray(times), cutil.toArray(numplainnop0));
		chart.saveAsPng(path, 800, 600);
	}
	public void analysePlans(){
		List<String> allincominglinksids = new ArrayList<String>();//Ids of incoming links in String form
		CollectionUtil<Person> cutil = new CollectionUtil<Person>();
		String path = "./ihop2/matsim-input/config - P0.xml";
//		String path = "H:\\Mike Work\\input\\config.xml";
		Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
	    scenario.getPopulation().getPersons().clear();
	    final PopulationReader popReader = new PopulationReader(
				scenario);
		popReader.readFile("C:\\Results Matsim\\matsim-output-statsandplots\\PNP01\\it.1000\\1000.plans.xml.gz");
//	    popReader.readFile("./ihop2/matsim-input/5.plans.xml.gz");
		Population population = scenario.getPopulation();
		ArrayList<Person> persons = cutil.toArrayList((Iterator<Person>) population.getPersons().values().iterator());
		String nodesfile = "./ihop2/matsim-input/Nodes.csv";
		Network network = (Network)scenario.getNetwork();
		StockholmP0Helper sth = new StockholmP0Helper(network);
		List<String> timednodes = sth.getPretimedNodes(nodesfile);
		Map<String, List<Link>> inlinksforjunctions = sth.getInLinksForJunctions(timednodes, network);
		Iterator<String> nodes = inlinksforjunctions.keySet().iterator();//List of incoming links for each pretimed node
		while(nodes.hasNext()){
			String node = nodes.next();
			List<Link> inlinks = inlinksforjunctions.get(node);
			if(inlinks.size()<2){
				System.out.println(node);
			}
			else{
				Iterator<Link> linksiter = inlinks.iterator();
				while(linksiter.hasNext()){
					allincominglinksids.add(linksiter.next().getId().toString());
				}
			}
		}
		boolean keep = false;
		int size = persons.size();
		for (int i=0;i<size;i++) {
			keep=false;
			Person person = ((Person)persons.get(i));
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for (PlanElement planelement : planElements) {
        		if (planelement instanceof Leg) {
        			Leg leg = (Leg) planelement;
        			String[] links = leg.getRoute().getRouteDescription().split(" ");//Sequence of link ids in string form split
        			int length = links.length;
        			for(int j=0;j<length;j++){
        				if(allincominglinksids.contains(links[j])){
        					keep = true;
        					break;
        				}
        			}
        		}
			}
			if(!keep){
				population.getPersons().remove(person.getId());
			}
		}
		double avgscore = 0;
		double avgdistancetravelled = 0;
		double avgtripduration = 0;
		Collection<? extends Person> allpersons = population.getPersons().values();
		for(Person person: allpersons){
			double tripduration = 0;
			double distancetravelled = 0;
			int legs = 0;
			List<PlanElement> planelements = person.getSelectedPlan().getPlanElements();
			for (PlanElement planelement : planelements) {
				if (planelement instanceof Leg) {
					legs++;
					Leg leg = (Leg) planelement;
	        		distancetravelled = distancetravelled + leg.getRoute().getDistance();
	        		tripduration = tripduration + leg.getTravelTime();
				}
			}
			tripduration=tripduration/legs;
			distancetravelled=distancetravelled/legs;
			avgdistancetravelled+=distancetravelled;
			avgtripduration+=tripduration;
			avgscore+=person.getSelectedPlan().getScore();
		}
		int totalagents = population.getPersons().size();
		System.out.println("Average Score: " + avgscore/totalagents);
		System.out.println("Average Distance Travelled: " + avgdistancetravelled/totalagents);
		System.out.println("Average Trip Duration: " + avgtripduration/totalagents);
		System.out.println("Total Agents: " + totalagents);
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write("./ihop2/matsim-input/relevantpopulation.xml");

	}
}
