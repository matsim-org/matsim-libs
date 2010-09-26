package playground.mmoyo.utils;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.api.core.v01.population.Population;

/**
 *  prints all agents id's in console
 */
public class PopulationList {
	private final Population population;
	final String SEPARATOR = ":\t ";
	
	public PopulationList(final Population population){	
		this.population= population;
	}
	
	public void ListPersons(){
		for(Person person: this.population.getPersons().values()){
			System.out.println(person.getId() +  SEPARATOR + Double.toString(person.getSelectedPlan().getScore()));
		}
	}
	
	public void SortAndListPersons(){
		Set<Id> keySet = this.population.getPersons().keySet();
		TreeSet<Id> treeSet = new TreeSet<Id>(keySet);
		
		int zeroFound =0;
		for (Id id : treeSet){
			double score = this.population.getPersons().get(id).getSelectedPlan().getScore();
			System.out.println(id +  SEPARATOR + Double.toString(score));
			if (score==0){
				zeroFound++;
				try {
					throw new RuntimeException("Score equals zero" );
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Number of persons: " + treeSet.size());
		System.out.println("scores that equal zero: " + zeroFound);
	}
	
	public static void main(String[] args) {
		String PopulationFile;
		if (args.length>0){
			PopulationFile = args[0];
		}else{
			PopulationFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/bestValues_plans.xml.gz";	
		}
		//read population file
		System.out.println("counting population for population file: " + PopulationFile);
		ScenarioImpl scenario = new ScenarioImpl();
		Population population = scenario.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(PopulationFile);
		
		PopulationList populationList = new PopulationList(population);
		populationList.ListPersons();
	}
}
