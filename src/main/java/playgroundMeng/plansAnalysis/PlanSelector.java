package playgroundMeng.plansAnalysis;

import java.util.List;

import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PlanSelector {
public static void main(String[] args) {
	
	
	String inputPlanFile = "C:/Users/VW3RCOM/Desktop/vw280_100pct.output_plans_selectedOnly.xml.gz";
	String outputPlanFile = "C:/Users/VW3RCOM/Desktop/VW280_LocalLinkFlow_1.28_100pct_2.output_plans.xml.gz";
	
	
	Config config = ConfigUtils.createConfig();
	config.plans().setInputFile(inputPlanFile);
	Scenario scenario = ScenarioUtils.loadScenario(config);
	Population population = scenario.getPopulation();
		
		Population outputPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		
		for (Person person : population.getPersons().values()) {
			
			Person outPerson = outputPopulation.getFactory().createPerson(person.getId());
			
			Plan selectedPlan = person.getSelectedPlan();
			selectedPlan.setScore(0.);
			
			Plan outputPlan = outputPopulation.getFactory().createPlan();
			PopulationUtils.copyFromTo(selectedPlan, outputPlan);
			outPerson.addPlan(outputPlan);
			outputPopulation.addPerson(outPerson);
			
//			int noOfPlans = person.getPlans().size();
//			
//			for (int index =0; index < noOfPlans; index++ ) {
//				
//				Plan plan = person.getPlans().get(index);
//				
//				if ( person.getSelectedPlan().equals(plan) ) {
//					//keep it
//				} else {
//					//remove it
//				}
//				
//			}
//			
//			List<PlanElement> planElements = selectedPlan.getPlanElements();
//			for (PlanElement pe : planElements) {
//				if (pe instanceof Activity) {
//					Activity act = (Activity) pe;
//					
//					act.getCoord();
//					act.getEndTime();
//					
//				} else if (pe instanceof Leg) {
//					Leg leg = (Leg) pe;
//					
//				} else {
//					throw new RuntimeException("Unrecognized plan element: "+pe);
//				}
//			}
			
		}
		
		new PopulationWriter(outputPopulation).write("C:/Users/VW3RCOM/Desktop/SelectInWith0Score.xml");
		
		
	}

}
