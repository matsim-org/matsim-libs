package playground.mmoyo.utils;

import java.util.Set;
import java.util.TreeSet;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
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
		final String strEmpty= "";
		for(Person person: this.population.getPersons().values()){
			String str_score = strEmpty;
			if (person.getSelectedPlan().getScore()!=null) str_score= Double.toString(person.getSelectedPlan().getScore());
			System.out.println(person.getId()/* +  SEPARATOR + str_score*/);
		}
		System.out.println("Num of agents: " +  this.population.getPersons().size());
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
		String populationFile;
		if (args.length>0){
			populationFile = args[0];
		}else{
			populationFile = "../playgrounds/mmoyo/output/cadyts/w6.0d0.0t1200.0_w10.0d0.0t240.0_w8.0d0.5t720.0_ver2_NoCLONES.xml.gz";	
		}
		System.out.println("counting population for population file: " + populationFile);
		Population population = new DataLoader().readPopulation(populationFile);
		new PopulationList(population).ListPersons();
	}
}
