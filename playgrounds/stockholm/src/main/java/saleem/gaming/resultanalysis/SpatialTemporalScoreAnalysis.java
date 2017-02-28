package saleem.gaming.resultanalysis;

import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.pt.utils.CreatePseudoNetwork;
/**
 * A class to calculate accessibility measure for gaming scenarios, 
 * based on average all day utility of travellers performing an activity in,
 * or travelling through a certain area, with in a certain time window, using a certain mode.
 * 
 * @author Mohammad Saleem
 */
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
	
	/* Calculate score of people with in the area of focus, in the considered time, 
	 * using a given mode and returns a tuple (number of relevant people, score)
	 */
	public String calculateScore(String mode, Coord origin, double gridsize, double fromtime, double totime){ 
		Set<Person> relevantpersons = getRelevantPersons(mode, origin, gridsize, fromtime, totime);
		double score = getScoreOfRelevantPersons(relevantpersons);
		System.out.println("The average score of relevant population is: " + score);
		return relevantpersons.size() + "," + score;
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
		/* Mode can be "car", "pt" or "both"
		   If changing scenario, change plans file, events file and cordinates where to check score (if needed); you will need to re-instantiate the stsanalyser object.
		   Scenario is changed when you need to switch from Max PT to Min PT, or you want to change population i.e. employed to unemployed or unemployed to mixed etc., 
		   or you want to increase population elsewhere i,e. check for population increase in Arstafaltet instead of Alvsjo. If you change PT measure, also change config file accordingly
		   between Max and Min accordingly.  
		   If only checking score at a different time or place or for different mode but within existing scenario, you won't need to re-instantiate the stsanalyser object,
		   just call the calculateScore() function with changed coordinates, mode and/or starting and end time.
		   Default scenario is Farsta Centrum, Max PT measure, Employed Population, PT mode, Morning Peak Hour (06:00 to 09:00)
		 */
		SpatialTemporalScoreAnalysis stsanalyser = new SpatialTemporalScoreAnalysis("C:\\Jayanth\\FarstaCentrum\\configFarstaCentrumMax.xml", "C:\\Jayanth\\FarstaCentrum\\employedmax\\1000.plans.xml.gz", 
				"C:\\Jayanth\\FarstaCentrum\\employedmax\\1000.events.xml.gz");
		stsanalyser.calculateScore("pt", new Coord(676397, 6571273), 1000,  21600, 32400);//06:00 to 09:00, Morning Peak
	}

}

