package saleem.p0.resultanalysis;

import java.io.File;
import java.io.FileOutputStream;
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
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.charts.XYLineChart;

import saleem.p0.stockholm.StockholmP0Helper;
import saleem.stockholmmodel.utils.CollectionUtil;
/**
 * A class to analyse network effects due to P0.
 * Covers a varied set of statistics for comparison with the fixed time policy.
 * 
 * @author Mohammad Saleem
 */
public class NetworkAnalysis {
	public static void main(String[] args){
		NetworkAnalysis analyser = new NetworkAnalysis();
//		analyser.analysePlans();
//		analyser.analyseEvents();
//		analyser.analysePlans();
		analyser.analyseEventsForTT();
	}
	public void analyseEventsForTT(){
		List<String> allincominglinksids = new ArrayList<String>();//Ids of incoming links in String form
		CollectionUtil<Person> cutil = new CollectionUtil<Person>();
		String path = "./ihop2/matsim-input/config - P0.xml";
//		String path = "H:\\Mike Work\\input\\config.xml";
		Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
	    scenario.getPopulation().getPersons().clear();
	    final PopulationReader popReader = new PopulationReader(
				scenario);
		popReader.readFile("C:\\P0 Paper Runs\\HINP08\\ITERS\\it.2000\\2000.plans.xml.gz");
//	    popReader.readFile("./ihop2/matsim-input/5.plans.xml.gz");
		Population population = scenario.getPopulation();
		double initialtotal = population.getPersons().size();
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
			if(keep){
				population.getPersons().remove(person.getId());
			}
		}
		final EventsManager fosomanager = EventsUtils.createEventsManager(config);
		EventsToScore scoring = EventsToScore.createWithScoreUpdating(scenario, new CharyparNagelScoringFunctionFactory(scenario), fosomanager);
		FOSOEffectsHandler fososohandler = new FOSOEffectsHandler(scoring, scenario.getPopulation().getPersons(),fosomanager );//first order second order effects of P0
		fosomanager.addHandler(fososohandler);
				final MatsimEventsReader reader = new MatsimEventsReader(fosomanager);
//				scoring.beginIteration(2001);
		reader.readFile("C:\\P0 Paper Runs\\HINP08\\ITERS\\it.2000\\2000.events.xml.gz");
//		scoring.finish();
		fososohandler.getTT();
		fososohandler.getTotalrips();
//		getScoreFromPopulation(scenario);
//		System.out.println("The total Population is: " + population.getPersons().size());
	}
	public void getScoreFromPopulation(Scenario scenario)  {
		CollectionUtil<Person> cutil = new CollectionUtil<Person>();
		double avgscore = 0;
		Collection<? extends Person> allpersons = scenario.getPopulation().getPersons().values();
		for(Person person: allpersons){
			avgscore+=person.getSelectedPlan().getScore();
		}
		double totalagents = scenario.getPopulation().getPersons().size();
		System.out.println("Average Score: " + avgscore/totalagents);
	}
	public void analyseEvents(){
		List<String> allincominglinksids = new ArrayList<String>();//Ids of incoming links in String form
		String path = "./ihop2/matsim-input/config - P0.xml";
//		String path = "./ihop2/matsim-input/configSingleJunction.xml";

		final Config config = ConfigUtils.loadConfig(path);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = (Network)scenario.getNetwork();
		StockholmP0Helper sth = new StockholmP0Helper(network);
		String nodesfile = "./ihop2/matsim-input/Nodes.csv";
//		String nodesfile = "./ihop2/matsim-input/NodesSingleJunction.csv";

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

		final EventsManager eventsNoP01 = EventsUtils.createEventsManager(config);
		EventsHandler handlerNoP01 = new EventsHandler(allincominglinksids, scenario.getNetwork());
		eventsNoP01.addHandler(handlerNoP01);
		final MatsimEventsReader readerNoP01 = new MatsimEventsReader(eventsNoP01);
		readerNoP01.readFile("C:\\P0 Paper Runs\\HIP08\\ITERS\\it.2000\\2000.events.xml.gz");
//		readerNoP01.readFile("C:\\P0 Paper Runs\\SingleJunction\\NP03\\ITERS\\it.500\\500.events.xml.gz");


		final EventsManager eventsNoP02 = EventsUtils.createEventsManager(config);
		EventsHandler handlerNoP02 = new EventsHandler(allincominglinksids, scenario.getNetwork());
		eventsNoP02.addHandler(handlerNoP02);
		final MatsimEventsReader readerNoP02 = new MatsimEventsReader(eventsNoP02);
		readerNoP02.readFile("C:\\P0 Paper Runs\\HINP02\\ITERS\\it.2000\\2000.events.xml.gz");
//		readerNoP02.readFile("C:\\P0 Paper Runs\\SingleJunction\\NP02\\ITERS\\it.500\\500.events.xml.gz");

		
		final EventsManager eventsNoP03 = EventsUtils.createEventsManager(config);
		EventsHandler handlerNoP03 = new EventsHandler(allincominglinksids, scenario.getNetwork());
		eventsNoP03.addHandler(handlerNoP03);
		final MatsimEventsReader readerNoP03 = new MatsimEventsReader(eventsNoP03);
		readerNoP03.readFile("C:\\P0 Paper Runs\\HINP03\\ITERS\\it.2000\\2000.events.xml.gz");
//		readerNoP03.readFile("C:\\P0 Paper Runs\\SingleJunction\\NP05\\ITERS\\it.500\\500.events.xml.gz");

		
		
		
		final EventsManager eventsP01 = EventsUtils.createEventsManager(config);
		EventsHandler handlerP01 = new EventsHandler(allincominglinksids, scenario.getNetwork());
		eventsP01.addHandler(handlerP01);
		final MatsimEventsReader readerP01 = new MatsimEventsReader(eventsP01);
		readerP01.readFile("C:\\P0 Paper Runs\\HIP01\\ITERS\\it.2000\\2000.events.xml");
//		readerP01.readFile("C:\\P0 Paper Runs\\SingleJunction\\P04\\ITERS\\it.500\\500.events.xml.gz");

		final EventsManager eventsP02 = EventsUtils.createEventsManager(config);
		EventsHandler handlerP02 = new EventsHandler(allincominglinksids, scenario.getNetwork());
		eventsP02.addHandler(handlerP02);
		final MatsimEventsReader readerP02 = new MatsimEventsReader(eventsP02);
		readerP02.readFile("C:\\P0 Paper Runs\\HIP02\\ITERS\\it.2000\\2000.events.xml.gz");
//		readerP02.readFile("C:\\P0 Paper Runs\\SingleJunction\\P08\\ITERS\\it.500\\500.events.xml.gz");

		
		final EventsManager eventsP03 = EventsUtils.createEventsManager(config);
		EventsHandler handlerP03 = new EventsHandler(allincominglinksids, scenario.getNetwork());
		eventsP03.addHandler(handlerP03);
		final MatsimEventsReader readerP03 = new MatsimEventsReader(eventsP03);
		readerP03.readFile("C:\\P0 Paper Runs\\HIP03\\ITERS\\it.2000\\2000.events.xml.gz");
//		readerP03.readFile("C:\\P0 Paper Runs\\SingleJunction\\P03\\ITERS\\it.500\\500.events.xml.gz");
		
		printPercentageImprovement(handlerP01.getDelays(), handlerP02.getDelays(), handlerP03.getDelays(),
				handlerNoP01.getDelays(), handlerNoP02.getDelays(), handlerNoP03.getDelays());
		printPercentageImprovement(handlerP01.getAgentsOnIncomingLinks(), handlerP02.getAgentsOnIncomingLinks(), handlerP03.getAgentsOnIncomingLinks(),
				handlerNoP01.getAgentsOnIncomingLinks(), handlerNoP02.getAgentsOnIncomingLinks(), handlerNoP03.getAgentsOnIncomingLinks());
		printPercentageImprovement(handlerP01.getNumAgentsThroughIntersections(), handlerP02.getNumAgentsThroughIntersections(), handlerP03.getNumAgentsThroughIntersections(),
				handlerNoP01.getNumAgentsThroughIntersections(), handlerNoP02.getNumAgentsThroughIntersections(), handlerNoP03.getNumAgentsThroughIntersections());
//		writeToTextFile(handlerNoP03.getTimes(), handlerNoP03.getDelays(), handlerNoP03.getDelays(), handlerNoP03.getDelays(),
		
		
//				handlerNoP03.getDelays(), handlerNoP03.getDelays(), handlerNoP03.getDelays(), "C:\\P0 Paper Runs\\Results.txt");

//		writeToTextFile(handlerNoP02.getTimes(), handlerP01.getDelays(), handlerP02.getDelays(), handlerP03.getDelays(),
//				handlerNoP01.getDelays(), handlerNoP02.getDelays(), handlerNoP03.getDelays(), "C:\\P0 Paper Runs\\SingleJunction\\Delays.txt");
//		
//		writeToTextFile(handlerNoP01.getTimes(), handlerP01.getAgentsOnIncomingLinks(), handlerP02.getAgentsOnIncomingLinks(), handlerP03.getAgentsOnIncomingLinks(),
//				handlerNoP01.getAgentsOnIncomingLinks(), handlerNoP02.getAgentsOnIncomingLinks(), handlerNoP03.getAgentsOnIncomingLinks(), "C:\\P0 Paper Runs\\OnIncomingLinksS.txt");
//		
//		writeToTextFile(handlerNoP01.getTimes(), handlerP01.getNumAgentsThroughIntersections(), handlerP02.getNumAgentsThroughIntersections(), handlerP03.getNumAgentsThroughIntersections(),
//				handlerNoP01.getNumAgentsThroughIntersections(), handlerNoP02.getNumAgentsThroughIntersections(), handlerNoP03.getNumAgentsThroughIntersections(), "C:\\P0 Paper Runs\\ThroughIntersectionsS.txt");


//		writeToTextFile(handlerP01.getTimes(), handlerP01.getDelays(), handlerP01.getDelays(), handlerP01.getDelays(),
//				handlerP01.getDelays(), handlerP01.getDelays(), handlerP01.getDelays(), "C:\\P0 Paper Runs\\Results.txt");
		
//		
//		plotDelays("./ihop2/matsim-input/delays.png",handlerP0.getTimes(), handlerP0.getDelays(),handlerPlainNoP01.getDelays());
//		plotTravellersThroughNodes("./ihop2/matsim-input/travellersthrooughnodes.png",handlerP0.getTimes(), handlerP0.getNumAgentsThroughIntersections(), handlerPlainNoP0.getNumAgentsThroughIntersections());
//		plotTravellersOnIncomingLinks("./ihop2/matsim-input/travellersonincominglinks.png",handlerP0.getTimes(), handlerP0.getAgentsOnIncomingLinks(),handlerPlainNoP0.getAgentsOnIncomingLinks());
		
		System.out.println();

	}
	public void printPercentageImprovement(ArrayList<Double> p01, ArrayList<Double> p02, ArrayList<Double> p03, 
			ArrayList<Double> np01, ArrayList<Double> np02, ArrayList<Double> np03){
		Iterator<Double> iterp01 = p01.iterator();
		Iterator<Double> iterp02 = p02.iterator();
		Iterator<Double> iterp03 = p03.iterator();
		Iterator<Double> iternp01 = np01.iterator();
		Iterator<Double> iternp02 = np02.iterator();
		Iterator<Double> iternp03 = np03.iterator();
		double delayP0=0;
		double delayFT=0;
		
		 while(iterp01.hasNext()){
			double d = iterp01.next();
			delayP0+=d;
		 }
		 while(iterp02.hasNext()){
			double d = iterp02.next();
			delayP0+=d;
		 }
		 while(iterp03.hasNext()){
			double d = iterp03.next();
			delayP0+=d;
		 }
		 while(iternp01.hasNext()){
			double d = iternp01.next();
			delayFT+=d;
		 }
		 while(iternp02.hasNext()){
			double d = iternp02.next();
			delayFT+=d;
		 }
		 while(iternp03.hasNext()){
			double d = iternp03.next();
			delayFT+=d;
		}
		 double improvement = ((delayFT-delayP0)/delayFT)*100;
		System.out.println("Average Improvement is: " + improvement);
	}
	public void writeToTextFile(ArrayList<Double> times, ArrayList<Double> p01, ArrayList<Double> p02, ArrayList<Double> p03, 
			ArrayList<Double> np01, ArrayList<Double> np02, ArrayList<Double> np03, String path){
		Iterator<Double> itertimes = times.iterator();
		Iterator<Double> iterp01 = p01.iterator();
		Iterator<Double> iterp02 = p02.iterator();
		Iterator<Double> iterp03 = p03.iterator();
		Iterator<Double> iternp01 = np01.iterator();
		Iterator<Double> iternp02 = np02.iterator();
		Iterator<Double> iternp03 = np03.iterator();
		
		
		try { 
			File file=new File(path);
			String text="";
			 while(itertimes.hasNext()){
				 
				 	double d = itertimes.next();
		        	text = text + d + "\t";
		        	
				 	if(iterp01.hasNext()){
				 		d = iterp01.next();
			        	text = text + d + "\t";
				 	}else{
				 		text = text + "\t";
				 	}
				 	if(iterp02.hasNext()){
				 		d = iterp02.next();
			        	text = text + d + "\t";
				 	}else{
				 		text = text + "\t";
				 	}
		        	
				 	if(iterp03.hasNext()){
				 		d = iterp03.next();
			        	text = text + d + "\t";
				 	}else{
				 		text = text + "\t";
				 	}
		        	
				 	if(iternp01.hasNext()){
				 		d = iternp01.next();
			        	text = text + d + "\t";
				 	}else{
				 		text = text + "\t";
				 	}
		        	
				 	if(iternp02.hasNext()){
				 		d = iternp02.next();
			        	text = text + d + "\t";
				 	}else{
				 		text = text + "\t";
				 	}
		        	
				 	if(iternp03.hasNext()){
				 		d = iternp03.next();
			        	text = text + d + "\n";
				 	}else{
				 		text = text + "\n";
				 	}
//		        	
		        }
		    FileOutputStream fileOutputStream=new FileOutputStream(file);
		    fileOutputStream.write(text.getBytes());
		    fileOutputStream.close();
	       
	    } catch(Exception ex) {
	        //catch logic here
	    }
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
		double passing = 0;
		List<String> allincominglinksids = new ArrayList<String>();//Ids of incoming links in String form
		CollectionUtil<Person> cutil = new CollectionUtil<Person>();
		String path = "./ihop2/matsim-input/config - P0.xml";
//		String path = "H:\\Mike Work\\input\\config.xml";
		Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
	    scenario.getPopulation().getPersons().clear();
	    final PopulationReader popReader = new PopulationReader(
				scenario);
		popReader.readFile("C:\\P0 Paper Runs\\HIP07\\ITERS\\it.2000\\2000.plans.xml.gz");
//	    popReader.readFile("./ihop2/matsim-input/5.plans.xml.gz");
		Population population = scenario.getPopulation();
		double initialtotal = population.getPersons().size();
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
			if(keep){
//				population.getPersons().remove(person.getId());
				passing++;
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
		double totalagents = population.getPersons().size();
		System.out.println("Average Passing Through Signalised Junction: " + passing/initialtotal);
		System.out.println("Average Score: " + avgscore/totalagents);
		System.out.println("Average Distance Travelled: " + avgdistancetravelled/totalagents);
		System.out.println("Average Trip Duration: " + avgtripduration/totalagents);
//		System.out.println("Total Agents: " + totalagents);
		MatsimWriter popWriter = new PopulationWriter(population, network);
//		popWriter.write("./ihop2/matsim-input/relevantpopulation.xml");

	}
}
