package playground.singapore.springcalibration.preprocess;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


public class PlanAdapter {
	
	private final static Logger log = Logger.getLogger(PlanAdapter.class);

	public static void main(String[] args) {
		PlanAdapter adapter = new PlanAdapter();
		adapter.run(args[0], args[1]);

	}
	
	public void run(String inputFile, String outFile) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inputFile);
		
		this.adapt(scenario.getPopulation());
		this.writePlans(scenario.getPopulation(), outFile);
		log.info("finished ##############################################");
		
	}
	
	private void adapt(Population population) {
		for (Person p : population.getPersons().values()) {
			
			Plan plan = new BestPlanSelector<Plan, Person>().selectPlan(p);
			plan.setScore(null);
			
			for (PlanElement pe : plan.getPlanElements()){	
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					leg.setRoute(null);
 				}
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					act.setEndTime(act.getEndTime() + 3600.0);
 				}
			}
			p.getPlans().clear();
			p.addPlan(plan);
			p.setSelectedPlan(plan);
		}
	}
	
	
	private void writePlans(Population population, String outFile) {
		PopulationWriter writer = new PopulationWriter(population);
		writer.write(outFile);
		log.info("writing plans to: " + outFile);
	}
}


