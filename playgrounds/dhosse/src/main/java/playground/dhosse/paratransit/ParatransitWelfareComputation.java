package playground.dhosse.paratransit;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class ParatransitWelfareComputation {

	private static String seed = "8192";
	
	private static final String paratransitFolder = "C:/Users/dhosse/workspace/shared-svn/projects/paratransit/";
	
	private static final String paratransitBaseFolder = paratransitFolder + "baseCase/scenarios/grid/output/baseCase_" + seed + "/baseCase";
	
	private static final String paratransitProfitFolder = paratransitFolder + "paratransitProfitExample/scenarios/grid/output/profitMaximization_" + seed + "/profitMaximization";
	
	public static void main(String args[]){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile(paratransitProfitFolder + "_" + seed + ".output_plans.xml.gz");
		
		double userBenefits = 0.;
		double operatorProfit = 0.;
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			userBenefits += person.getSelectedPlan().getScore();
			
		}
		
		System.out.println("user benefits: " + userBenefits);
		
	}
	
}
