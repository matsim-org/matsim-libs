package saleem.gaming.resultanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import saleem.stockholmmodel.utils.CollectionUtil;
/**
 * A class to do basic plausibility checks and result analysis over the executed gaming simulation scenarios, 
 * before the results were provided to ProtoWorld to be used in interactive gaming.
 * E.g. trip distances, trip durations, scores etc. are checked.
 * 
 * @author Mohammad Saleem
 */
public class AddedPopulationAnalysis {
	public static void main(String[] args){
		
		String planspath = "C:\\Jayanth\\Arstafaltet\\unemployedmin\\1000.plans.xml.gz";
		String eventspath = "C:\\Jayanth\\Arstafaltet\\unemployedmin\\1000.events.xml.gz";
		    
		    
		AddedPopulationAnalysis analyser = new AddedPopulationAnalysis();
		analyser.analyseEventsForTripDuration(planspath,eventspath );
		analyser.analysePlansScore(planspath);
//		analyser.analyseModalSplit(planspath);
//		analyser.analyseDistancesTravelled(planspath);
	}
	public void analyseEventsForTripDuration(String planspath, String eventspath){
		String path = "./ihop2/matsim-input/config.xml";
		Config config = ConfigUtils.loadConfig(path);
	    final EventsManager manager = EventsUtils.createEventsManager(config);
	    
	    ArrayList<Double> list = this.analyseModalSplit(planspath);//Use plans for same scenario as events, by default only considers added population
	    
	    double cartrips = list.get(0); 
	    double pttrips = list.get(1);
	    double trips = cartrips + pttrips;
	    
	    AddedPopulationEventHandler handler = new AddedPopulationEventHandler(trips, cartrips, pttrips);//By default only considers added population
		manager.addHandler(handler);

		final MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile(eventspath);
		
		handler.printStuckAgentsInfo();
		handler.printTripDurations();

	}
	public void analyseDistancesTravelled(String planspath){
		CollectionUtil<Person> cutil = new CollectionUtil<Person>();
		double totaldist = 0;
		int personcount = 0;
		String path = "./ihop2/matsim-input/config.xml";//Any working config file
		Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
	    
	    scenario.getPopulation().getPersons().clear();
	    final PopulationReader popReader = new PopulationReader(
				scenario);
	    
	    popReader.readFile(planspath);
	    
		Population population = scenario.getPopulation();
		ArrayList<Person> persons = cutil.toArrayList((Iterator<Person>) population.getPersons().values().iterator());
		int size = persons.size();
		for (int i=0;i<size;i++) {
			Person person = ((Person)persons.get(i));
			if(person.getId().toString().contains("a")){ //Only consider added population
				int activityct = 0;
				personcount++;//
				double totallegtime=0;
				List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
				for (PlanElement planelement : planElements) {
					if (planelement instanceof Activity){
						Activity activity = (Activity)planelement;
					 	if((!(activity.getType().toString().contains("pt interaction")))){//Only home, work or other activitites. Not PT boardings etc.
					 		activityct++;
					 	}
					}
					if (planelement instanceof Leg){
						Leg leg = (Leg) planelement;
						totallegtime += leg.getRoute().getDistance();
					}
				}
				totaldist+=(totallegtime/(activityct-1));
			}
		}
		double avgdist = totaldist/personcount;
		System.out.println("Average distance travelled is: " + avgdist);
	}
	public ArrayList<Double> analyseModalSplit(String planspath){
		CollectionUtil<Person> cutil = new CollectionUtil<Person>();
		String path = "./ihop2/matsim-input/config.xml";//Any working config file
		double carcount = 0;
		double ptcount = 0;
		
		Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
	    
	    scenario.getPopulation().getPersons().clear();
	    final PopulationReader popReader = new PopulationReader(
				scenario);
	    
	    popReader.readFile(planspath);
	    
	    Population population = scenario.getPopulation();
	    
		ArrayList<Person> persons = cutil.toArrayList((Iterator<Person>) population.getPersons().values().iterator());
		int size = persons.size();
		for (int i=0;i<size;i++) {
			Person person = ((Person)persons.get(i));
			if(person.getId().toString().contains("a")){//Only added person
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for (PlanElement planelement : planElements) {
				if (planelement instanceof Activity){
					Activity activity = (Activity)planelement;
				 	if((!activity.getType().toString().contains("pt interaction"))){
				 		Leg leg = PopulationUtils.getNextLeg(person.getSelectedPlan(), (Activity)planelement);
				 		if(leg!=null){
				 			if(leg.getMode().equals("car")){//Using car
				 				carcount++;
				 			}
				 			else if(leg.getMode().equals("pt") || leg.getMode().equals("transit_walk")){//Using PT
				 				ptcount++;
				 			}
				 		}
				 	}
				}
			}
			}
		}
		double carpct = carcount/(carcount + ptcount)*100;//Car percentage
		double ptpct = ptcount/(carcount + ptcount)*100;
		ArrayList<Double> list = new ArrayList<Double>();
		list.add(carcount);
		list.add(ptcount);
		
		System.out.println("Car Trips Num is: " + carcount);
		System.out.println("PT Trips Num is: " + ptcount);
		System.out.println("Car Trips % is: " + carpct);
		System.out.println("PT Trips % is: " + ptpct);
		 return list;

	}
	public void analysePlansScore(String planspath){

		CollectionUtil<Person> cutil = new CollectionUtil<Person>();
		double totalscore = 0,totalscorecar = 0, totalscorept = 0;
		int personcount = 0,countcar = 0,countpt = 0;
		
		String path = "./ihop2/matsim-input/config.xml";//Any config file, doesnt matter, the population is cleared anyways.
		Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
	    scenario.getPopulation().getPersons().clear();
	    final PopulationReader popReader = new PopulationReader(
				scenario);
	    
	    popReader.readFile(planspath);

		Population population = scenario.getPopulation();
		ArrayList<Person> persons = cutil.toArrayList((Iterator<Person>) population.getPersons().values().iterator());
		int size = persons.size();
		for (int i=0;i<size;i++) {
			Person person = ((Person)persons.get(i));
			if(person.getId().toString().contains("a")){ //Added population
				totalscore += person.getSelectedPlan().getScore();
				personcount+=1;
				
			}
			//Added population using car
			if(person.getId().toString().contains("a") && (PopulationUtils.getLegs(person.getSelectedPlan()).get(0).getMode().equals("car"))){
				totalscorecar += person.getSelectedPlan().getScore();
				countcar+=1;
			}
			//Added population using pt
			if(person.getId().toString().contains("a") && (PopulationUtils.getLegs(person.getSelectedPlan()).get(0).getMode().equals("pt")
					|| PopulationUtils.getLegs(person.getSelectedPlan()).get(0).getMode().equals("transit_walk"))){
				totalscorept += person.getSelectedPlan().getScore();
				countpt+=1;
			}
		}
		double avgscore = totalscore/personcount;
		double avgscorecar = totalscorecar/countcar;
		double avgscorept = totalscorept/countpt;
		
		System.out.println("Average score of added agents is: " + avgscore);
		System.out.println("Average score of added car agents is: " + avgscorecar);
		System.out.println("Average score of added pt agents is: " + avgscorept);
	}
}
