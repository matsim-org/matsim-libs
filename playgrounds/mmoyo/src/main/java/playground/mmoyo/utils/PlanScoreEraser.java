package playground.mmoyo.utils;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PersonImpl;

public class PlanScoreEraser {

	private Population removeScores(Population population){
		System.out.println("removing scores" + population.getPersons().size());
		
		Population newPopulation = new PopulationImpl(new ScenarioImpl());
		
		for (Person person : population.getPersons().values()) {
			PersonImpl newPerson = new PersonImpl(person.getId());
			PlanImpl newPlan = newPerson.createAndAddPlan(true);
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
				newPlan.getPlanElements().add(pe);
			}
			newPopulation.addPerson(newPerson);
		}
		return newPopulation;
	}
	
	private void setScores2Zero(Population population){
		System.out.println("Setting all scores to zero: " + population.getPersons().size());
		Double zero = 0.0;
		for (Person person : population.getPersons().values()) {
			for (Plan plan :person.getPlans()){
				plan.setScore(zero);
			}
		}
	}

	public static void main(String[] args) {
		String populationFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/normal_fast_minTra_routes.xml.gz";
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String outputFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/normal_fast_minTra_routes_scoreZero.xml";
		
		ScenarioImpl scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
		Population population = scenario.getPopulation();
		
		new PlanScoreEraser().setScores2Zero(population);
		System.out.println("writing output plan file..." + outputFile);
		new PopulationWriter(population, scenario.getNetwork()).write(outputFile);
		System.out.println("done");
	}

}
