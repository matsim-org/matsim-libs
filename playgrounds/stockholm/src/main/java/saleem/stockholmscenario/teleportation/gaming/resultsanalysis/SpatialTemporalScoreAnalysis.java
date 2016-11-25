package saleem.stockholmscenario.teleportation.gaming.resultsanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

import saleem.stockholmscenario.utils.CollectionUtil;

public class SpatialTemporalScoreAnalysis {
	
	
	private final String configfile;
	private final String plansfile;
	private final String eventsfile;
	private Scenario scenario;
	
	public SpatialTemporalScoreAnalysis(String configfile, String plansfile, String eventsfile){
		this.configfile=configfile;
		this.plansfile=plansfile;
		this.eventsfile=eventsfile;
		this.scenario = loadScenario();
	}
	
	public Scenario loadScenario(){//Load the convereged population
		Config config = ConfigUtils.loadConfig(configfile);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
	    System.out.println("Initial Size of Population: " + scenario.getPopulation().getPersons().size());
	    scenario.getPopulation().getPersons().clear();
	    final PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(plansfile);
		System.out.println("Final Size of Population: " + scenario.getPopulation().getPersons().size());
		new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "tr_").createNetwork();
		return scenario;
	}
	
	//Calculate score of people with in the area of focus, in the considered time, using a given mode
	public void calculateScore(String mode, Coord origin, double gridsize, double fromtime, double totime){ 
		Set<Person> relevantpersons = getRelevantPersons(mode, origin, gridsize, fromtime, totime);
//		Iterator<Id<Person>> iter = scenario.getPopulation().getPersons().keySet().iterator();
//		while(iter.hasNext()){
//			Id<Person>  id = iter.next();
//			if(!relevantpersons.contains(id)){
//				System.out.println(iter.next());
//			}
//		}
		double score = getScoreOfRelevantPersons(relevantpersons);
		System.out.println("The average score of relevant population is: " + score);
	}
	
	//return people with in the area of focus, in the considered time, using a given mode
	public Set<Person> getRelevantPersons(String mode, Coord origin, double gridsize, double fromtime, double totime){
		final EventsManager eventsmanager = EventsUtils.createEventsManager(scenario.getConfig());
	    EventsToScore scoring = EventsToScore.createWithScoreUpdating(scenario, new CharyparNagelScoringFunctionFactory(scenario), eventsmanager);
		SpatialTemporalEventHandler eventshandler = new SpatialTemporalEventHandler(scoring, scenario, mode, origin, gridsize, fromtime, totime);
		eventsmanager.addHandler(eventshandler);
		final MatsimEventsReader reader = new MatsimEventsReader(eventsmanager);
		scoring.beginIteration(1001);//Scoring from events to get experienced scores, instead of planned scores.
		reader.readFile(eventsfile);
		scoring.finish();
		return eventshandler.getRelevantPersons();
	}

	//Calculate score of filtered persons
	public Double getScoreOfRelevantPersons(Set<Person> relevantpersons)  {
		Iterator<Person> relperiter = relevantpersons.iterator();
		double avgscore = 0;
		while(relperiter.hasNext()){
			avgscore+=relperiter.next().getSelectedPlan().getScore();
		}
		double totalagents = relevantpersons.size();
		return avgscore/totalagents;
	}
	
	public static void main(String[] args){
		//Mode can be "car", "pt" or "both"
		SpatialTemporalScoreAnalysis stsanalyser = new SpatialTemporalScoreAnalysis("C:\\Jayanth\\FarstaCentrum\\configFarstaCentrumEmployed10pcMax.xml", "C:\\Jayanth\\FarstaCentrum\\employedmax\\1000.plans.xml.gz", 
				"C:\\Jayanth\\FarstaCentrum\\employedmax\\1000.events.xml.gz");
		stsanalyser.calculateScore("both", new Coord(676397, 6571273), 1000000,  0, 110000);//28800, 36000
	}

}

