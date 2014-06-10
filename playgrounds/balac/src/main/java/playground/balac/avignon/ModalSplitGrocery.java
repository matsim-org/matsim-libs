package playground.balac.avignon;

import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.retailers.utils.ModalSplit;

public class ModalSplitGrocery {
	ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	PopulationReader populationReader = new MatsimPopulationReader(scenario);
	
	public void run(String plansFilePath) throws IOException {
		populationReader.readFile(plansFilePath);

		int count = 0;
		int countCar = 0;
		int countBike = 0;
		int countWalk = 0;
		int countPt = 0;
		
		Population pop = scenario.getPopulation();	
		for (Person p:pop.getPersons().values()) {
			Leg previousLeg = null;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					previousLeg = (Leg) pe;
					
					if (previousLeg.getMode().equals( "car" )) {
						countCar++;
						count++;
					}
					else if (previousLeg.getMode().equals("bike")) {
						countBike++;
						count++;
					}
					else if (previousLeg.getMode().equals("walk")) {
						countWalk++;
						count++;
					}
					else if (previousLeg.getMode().equals("pt")) {
						countPt++;
						count++;
					}
				}
				else if (pe instanceof Activity) {
					
					//if (((Activity) pe).getType() == "shopgrocery") {
					
						
						
					//}
				}
				
			}
			
		}
		System.out.println(count);
		System.out.println((double)countCar/(double)count);
		System.out.println((double)countBike/(double)count);
		System.out.println((double)countWalk/(double)count);
		System.out.println((double)countPt/(double)count);
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ModalSplitGrocery m = new ModalSplitGrocery();
		m.run(args[0]);
	}
	
	
}
