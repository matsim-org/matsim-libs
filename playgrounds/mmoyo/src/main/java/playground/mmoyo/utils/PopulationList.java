package playground.mmoyo.utils;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;

import playground.mmoyo.io.TextFileWriter;

/**  prints all agents id's */
	public class PopulationList {
	private final Population population;
	final String SEPARATOR = ":\t ";
	
	public PopulationList(final Population population){	
		this.population= population;
	}
	
	/**print id's in console*/
	public void ListPersons(){
		//final String strEmpty= "";
		for(Id id: this.population.getPersons().keySet()){
			//if (person.getSelectedPlan().getScore()!=null) str_score= Double.toString(person.getSelectedPlan().getScore());
			System.out.println(id /* +  SEPARATOR + str_score*/);
		}
		System.out.println("Num of agents: " +  this.population.getPersons().size());
	}
	
	/**saves id's in text file */
	public void SavePersonsIds(final String outFile){
		final String NR= "\n";
		StringBuffer sBuff = new StringBuffer();
		for(Id id: this.population.getPersons().keySet()){
			sBuff.append(id + NR);
		}
		
		//write to output text file
		new TextFileWriter().write(sBuff.toString(),outFile,true);
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
			populationFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/baseplan_5x_subset_xy2links_ptplansonly.xml.gz";	
		}
		System.out.println("counting population for population file: " + populationFile);
		Population population = new DataLoader().readPopulation(populationFile);
		//new PopulationList(population).SavePersonsIds("../../input/bestValues_plansIDS.txt");
		new PopulationList(population).ListPersons();
	}
}