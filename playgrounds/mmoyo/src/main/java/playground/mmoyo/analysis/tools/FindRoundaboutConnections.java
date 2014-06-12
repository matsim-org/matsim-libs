package playground.mmoyo.analysis.tools;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.PlanFragmenter;

import java.util.ArrayList;
import java.util.List;

/**finds connections with transfer point that are farther than the destination point **/ 
public class FindRoundaboutConnections {
	
	private Population createDetouredPlan (final ScenarioImpl scenario){
        Population detouredPopulation = PopulationUtils.createPopulation(scenario.getConfig(), scenario.getNetwork());

		Population fragmentedPopulation = new PlanFragmenter().run(scenario.getPopulation());  //fragmented plans! otherwise it analyzes only the first leg
		
		for (Person person : fragmentedPopulation.getPersons().values() ){
			Plan plan =  person.getSelectedPlan();   
			
			List<Leg> legList = new ArrayList<Leg>();
			List<Activity> transitActList = new ArrayList<Activity>();

			List<PlanElement> elemList = plan.getPlanElements();
			Activity aAct = ((Activity)elemList.get(0));
			Activity bAct = ((Activity)elemList.get(elemList.size()-1));

			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					Activity act = (Activity)pe;
					if (!(act == aAct || act==bAct)){ 
						transitActList.add(act);
					}
				}else{
					
					Leg leg = (Leg)pe;
					leg.getTravelTime();
					//System.out.println ("distance: " + leg.getRoute().getDistance());
					//System.out.println ("time: " + leg.getTravelTime());
					
					legList.add((Leg)pe);
				}
			}
			double abDistance = CoordUtils.calcDistance(aAct.getCoord(), bAct.getCoord());

			//compare abDistance vs transfer points distances
			for (Activity trAct : transitActList ){
				double a_TransDistance = CoordUtils.calcDistance(aAct.getCoord() , trAct.getCoord());
				double b_TransDistance = CoordUtils.calcDistance(bAct.getCoord() , trAct.getCoord());
				if(a_TransDistance > abDistance || b_TransDistance > abDistance){
					if (!detouredPopulation.getPersons().keySet().contains(person.getId()))
						detouredPopulation.addPerson(person);
				}
			}
		}
		
		return detouredPopulation;
	}
	
	public static void main(String[] args) {
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		
		ScenarioImpl scenario = new DataLoader().loadScenario(configFile);
		Population detouredPopulation = new FindRoundaboutConnections().createDetouredPlan(scenario);
		
		System.out.println("writing detoured population plan file in output folder..." );
		PopulationWriter popwriter = new PopulationWriter(detouredPopulation, scenario.getNetwork());
		popwriter.write(scenario.getConfig().controler().getOutputDirectory() + "detouredPopulation.xml") ;
		System.out.println("done");
	}
	
}
