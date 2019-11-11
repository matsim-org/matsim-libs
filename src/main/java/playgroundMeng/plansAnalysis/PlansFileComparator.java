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
import org.matsim.core.scenario.ScenarioUtils;

public class PlansFileComparator {
	

public static void main(String[] args) {
		
	String inputPlanFile = "C:/Users/VW3RCOM/Desktop/vw280_100pct.output_plans_selectedOnly.xml.gz";
	String outputPlanFile = "C:/Users/VW3RCOM/Desktop/VW280_LocalLinkFlow_1.28_100pct_2.output_plans.xml.gz";
	
	Population inputPop = readPlansFile(inputPlanFile);
	Population outputPop = readPlansFile(outputPlanFile);

	System.out.println("beginn");
	for(Person person: inputPop.getPersons().values()) {
		Plan inputPlan = person.getSelectedPlan();
		Plan outputPlan = outputPop.getPersons().get(person.getId()).getSelectedPlan();
		
		if(!(inputPlan.getPlanElements().toString()).equals(outputPlan.getPlanElements().toString())) {
			System.out.println(person.getId());
			System.out.println(inputPlan.getPlanElements().toString());
			System.out.println(outputPlan.getPlanElements().toString());
		}
	}
	System.out.println("finish");			
	}

	public static Population readPlansFile(String string) {
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(string);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		
		return population;
	}
	

}

