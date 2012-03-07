package playground.mmoyo.utils;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class HomePlanDetector {
	final String strHome = "home";
	
	public boolean isHomePlan(final Plan plan){
		List<PlanElement> peList =plan.getPlanElements();
		if(peList.size()!=3 ){
			return false;
		} 
		if (!(peList.get(0) instanceof Activity && peList.get(2) instanceof Activity)){
			return false;
		}
		if ( !(  (Activity)peList.get(0)).getType().equals(strHome) && ((Activity) peList.get(2)).getType().equals(strHome)  ){
			return false;
		}
		if (!(peList.get(1) instanceof Leg)){
			return false;
		}
		if (!((Leg)peList.get(1)).getMode().equals(TransportMode.walk)){
			return false;
		}
		return true;		
	}
	
	private void agentsWithHomePlanSelected(Population pop){
		String esp = " ";
		for (Person person : pop.getPersons().values()){
			if (isHomePlan(person.getSelectedPlan())){
				System.out.println(person.getId() + esp + strHome);	
			}
		}		
		
	}
	
	public static void main(String[] args) {
		String popFilePath = "../../input/sep/bvgDemand/1000.plans.xml.gz";
		String netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		DataLoader dataloader = new DataLoader();
		Scenario scn = dataloader.readNetwork_Population(netFilePath, popFilePath);
		Population pop = scn.getPopulation();
		scn= null;
		new HomePlanDetector().agentsWithHomePlanSelected(pop);
	}

}