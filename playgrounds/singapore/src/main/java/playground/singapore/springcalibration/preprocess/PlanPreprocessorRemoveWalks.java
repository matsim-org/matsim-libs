package playground.singapore.springcalibration.preprocess;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


public class PlanPreprocessorRemoveWalks {
	
	private final static Logger log = Logger.getLogger(PlanPreprocessorRemoveWalks.class);

	public static void main(String[] args) {
		PlanPreprocessorRemoveWalks corrector = new PlanPreprocessorRemoveWalks();
		corrector.run(args[0], args[1]);
	}
	
	public void run(String plansFile, String plansOutFile) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MutableScenario scenarioTmp = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		new MatsimPopulationReader(scenarioTmp).readFile(plansFile);
		
		this.keepBestOnly(scenario, scenarioTmp);
		this.correctForLongWalks(scenario);
		this.writePlans(scenario.getPopulation(), scenario.getNetwork(), plansOutFile);
		log.info("finished ###################################################");	
	}
	
	private void keepBestOnly(MutableScenario scenario, MutableScenario scenarioTmp) {
		BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<Plan, Person>();
		Population population = scenarioTmp.getPopulation();
		
		for (Person person : population.getPersons().values()) {
			Plan bestPlan = bestPlanSelector.selectPlan(person);
			bestPlan.setScore(null);
			
			person.getPlans().clear();
			
			scenario.getPopulation().addPerson(person);
			person.addPlan(bestPlan);
			person.setSelectedPlan(bestPlan);
		}	
	}
	
	private void correctForLongWalks(MutableScenario scenario) {
		
		Population population = scenario.getPopulation();
		
		int cntCorrectLegs = 0;
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			
			for (PlanElement pe : plan.getPlanElements()){	
				if(pe instanceof Leg){
					Leg leg = ((Leg) pe);
					String mode = leg.getMode();
					double travelTime = leg.getTravelTime();
					
					if (travelTime > 60.0 * 60.0 && mode.equals(TransportMode.walk)) {
						leg.setMode(TransportMode.other);
						cntCorrectLegs++;
					}				
 				}
			}	
		}
		log.info(cntCorrectLegs + " legs corrected!");
	}

	
	private void writePlans(Population population, Network network, String outFile) {
		PopulationWriter writer = new PopulationWriter(population);
		writer.write(outFile);
	}

}
