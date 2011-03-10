package playground.mmoyo.utils;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 *  List agents whose id start with a given criterion 
 */
public class Agents_bySuffix {
	private final Population population;
	final String SEPARATOR = ":\t ";
	
	public Agents_bySuffix(final Population population){	
		this.population= population;
	}
	
	
	public void ListPersons(final String criterion){
		Map <String, Integer> idMap = new TreeMap <String, Integer>();
		for(Person person: this.population.getPersons().values()){
			String strid = person.getId().toString();
			String substr = strid.substring(0, strid.indexOf(criterion));
			if (!idMap.containsKey(substr)){
				idMap.put(substr, 0);
			}
			int v= idMap.get(substr);
			idMap.put(substr, ++v);
			System.out.println (strid + " " + substr + " " + v);
		}
		
		int sum=0;
		for (int val: idMap.values()){
			sum += val;
		}
		System.out.println(sum/idMap.size());
	
	}
	
	public static void main(String[] args) {
		String populationFile;
		if (args.length>0){
			populationFile = args[0];
		}else{
			populationFile = "../playgrounds/mmoyo/output/precalculation/routed3150/allRoutes3150.xml";
		}
		final String criterion = "_";
		System.out.println("showing selected agents: " + populationFile);
		Population population = new DataLoader().readPopulation(populationFile);
		new Agents_bySuffix(population).ListPersons(criterion);
	}
}
