package playground.artemc.plansTools;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;



public class PlanFileCompare {
	
	
	public static void main(String[] args){	
	
	
	int cartrips=0;
	int pttrips=0;
	int plan1Cartrips=0;
	int plan1Pttrips=0;
	int plan2Cartrips=0;
	int plan2Pttrips=0;
	String activity="";
	String prevActivity="";
	Scenario scenario1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario1).readFile("H:/MATSimSimuliations/output_10000_19788pt_links_FC/it.0/0.plans.xml/0.plans.xml");
	new PopulationReader(scenario2).readFile("H:/MATSimSimuliations/output_10000_19788pt_links_FC/it.0/0.plans.xml/0.plans.xml");
	Population population1 = scenario1.getPopulation();
	Population population2 = scenario1.getPopulation();

	
	
	for(Person agent:population1.getPersons().values()){
		for(PlanElement element:agent.getSelectedPlan().getPlanElements()){
				if(element instanceof Activity){
					prevActivity=activity;
					activity = ((Activity)element).getType();					
				}					
				if(element instanceof Leg){
					String mode = ((Leg)element).getMode();
					if(mode=="car"){
						cartrips++;
					    plan1Cartrips++;
				    }
					else if (mode=="pt"  && prevActivity!="pt interaction" ){
						pttrips++;
						plan1Pttrips++;
					}
			}
		}
			
		for(PlanElement element2:population2.getPersons().get(agent.getId()).getSelectedPlan().getPlanElements()){
			if(element2 instanceof Activity){
				prevActivity=activity;
				activity = ((Activity)element2).getType();					
			}					
			if(element2 instanceof Leg){
				String mode = ((Leg)element2).getMode();
				if(mode=="car"){
					cartrips++;
					plan2Cartrips++;
				}
				else if (mode=="pt"  && prevActivity!="pt interaction" ){
					pttrips++;
					plan2Pttrips++;
				}
					
			}	
		}
		
	}
	System.out.println(cartrips);
	System.out.println(pttrips);
	
	
	//PopulationReaderMatsimV5 plans = new PopulationReaderMatsimV5(scenario1);
	
}	
	
}
