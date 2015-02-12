package playground.artemc.plansTools;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;



public class PlanFileAnalyzer {
	
	
	public static void main(String[] args){	
	
	
	int cartrips=0;
	int pttrips=0;
	String activity="";
	String prevActivity="";
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReaderMatsimV5(scenario).parse("H:/MATSimSimuliations/output_10000_19788pt_links_FC/it.0/0.plans.xml/0.plans.xml");
	Population population = ((ScenarioImpl) scenario).getPopulation();

	for(Person agent:population.getPersons().values()){
		for(Plan plan:agent.getPlans()){
			for(PlanElement element:plan.getPlanElements()){
				if(element instanceof Activity){
					prevActivity=activity;
					activity = ((Activity)element).getType();					
				}					
				if(element instanceof Leg){
					String mode = ((Leg)element).getMode();
					if(mode=="car")
						cartrips++;
					else if (mode=="pt"  && prevActivity!="pt interaction" )
						pttrips++;
				}
			}
			
		}
		
	}
	System.out.println(cartrips);
	System.out.println(pttrips);
	
	
	//PopulationReaderMatsimV5 plans = new PopulationReaderMatsimV5(scenario1);
	
}	
	
}
