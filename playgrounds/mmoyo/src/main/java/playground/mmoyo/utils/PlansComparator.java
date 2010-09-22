package playground.mmoyo.utils;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

public class PlansComparator {

	/**
	 * Compares person per person two populations 
	 */
	public static void main(String[] args) {
		ScenarioImpl scenario1 = new ScenarioImpl();

		//read population1
		String plansFile1 = "../../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/timeAllocTest/a_10plans.xml.gz";
		Population population1 = scenario1.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(scenario1);
		popReader.readFile(plansFile1);

		//read population2
		String plansFile2 = "../../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/500.plans.xml.gz";
		ScenarioImpl scenario2 = new ScenarioImpl();
		Population population2 = scenario2.getPopulation();
		popReader = new MatsimPopulationReader(scenario2);
		popReader.readFile(plansFile2);
		
		int similar=0;
		int dissimilar = 0;
		String Equals = " equals";
		String NoEquals = " does not equal";
		for (Person person1 : population1.getPersons().values()){
			Person person2 = population2.getPersons().get(person1.getId());
			boolean scoreEquals = Double.compare(person1.getSelectedPlan().getScore() , person2.getSelectedPlan().getScore())==0;
			if (scoreEquals){
				similar++;
			}else{
				dissimilar++;
			}
			System.out.println(person1.getId() + (scoreEquals? Equals: NoEquals));
		}
		System.out.println(" similar: " + similar + "\ndisimilar: " + dissimilar);
	}

}

