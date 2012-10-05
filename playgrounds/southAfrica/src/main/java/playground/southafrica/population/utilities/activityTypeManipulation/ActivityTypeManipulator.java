package playground.southafrica.population.utilities.activityTypeManipulation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

/**
 * Once a population has been generated, the {@link Activity} types often 
 * requires adaption to account for different durations. For example, when
 * <i>shopping</i> (s) duration has a large time distribution, it may be  
 * useful to change them to s1, s2, etc., each with a different <i>typical
 * duration</i>. This may result in more accurate simulation results.
 *
 * @author jwjoubert
 */
public abstract class ActivityTypeManipulator {
	private Logger log = Logger.getLogger(ActivityTypeManipulator.class);
	private Scenario sc;
	
	
	protected void parsePopulation(String population, String network){
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		/* Read the population file. */
		MatsimPopulationReader mpr = new MatsimPopulationReader(sc);
		mpr.readFile(population);
		
		/* Read the network file. */
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc);
		mnr.readFile(network);		
		
		log.info("Population: " + sc.getPopulation().getPersons().size());
		log.info("Network: " + sc.getNetwork().getNodes().size() + " nodes; " 
				+ sc.getNetwork().getLinks().size() + " links");
	}
	
	
	/**
	 * Runs through the entire {@link Population}, adapting the activity type 
	 * of each {@link Person}'s selected {@link Plan} based on the area-specific 
	 * implementation of {@link #getAdaptedActivityType(String, double)} method.
	 */
	protected void run(){
		log.info("Start manipulating person plans...");
		Counter counter = new Counter("  person # ");
		if(sc == null){
			throw new RuntimeException("Cannot process the population if it has " +
					"not been parsed yet. First run the parsePopulation() method.");
		} else{
			for(Person person : sc.getPopulation().getPersons().values()){
				if(person.getPlans().size() > 1){
					log.warn("Person " + person.getId() + " has multiple plans. " +
							"Only the selected plan will be adapted.");
				}
				this.adaptActivityTypes(person.getSelectedPlan());
				counter.incCounter();
			}
		}
		counter.printCounter();
		log.info("Done manipulating plans.");
	}
	
	protected Scenario getScenario(){
		return this.sc;
	}
	
	abstract protected void adaptActivityTypes(Plan plan);

}
