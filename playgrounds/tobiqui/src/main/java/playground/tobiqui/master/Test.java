package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 */

/**
 * @author tquick
 *
 */
public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String input = "D:/MA/workspace.bak/master/output/siouxfalls-2014/Test.xml";
		String output = "D:/MA/workspace.bak/master/output/siouxfalls-2014/TestSumo.xml";
		String populationOutput = "D:/MA/workspace.bak/master/output/siouxfalls-2014/TestPop.xml"; //if inputPopulation (input) NOT already sorted by end_times of first activity of selectedPlans:
		
		Map<Id<Person>, Person> persons = new HashMap<Id<Person>, Person>(); 
		Map<Id<Person>, Person> personsSorted = new LinkedHashMap<Id<Person>, Person>(); //id's sorted by end_times of first activity of selectedPlans 
		
		TqMatsimPlansReader pr = new TqMatsimPlansReader();
		persons = pr.getPlans(input); //insert input
		System.out.println("getPlans completed");
		
	//if inputPopulation (input) NOT already sorted by end_times of first activity of selectedPlans:
		personsSorted = pr.sortPlans(persons);
		System.out.println("sortPlans completed");
		pr.writeSortedPopulation(pr.getSortedPopulation(), populationOutput); //insert output for sorted Population
		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(personsSorted, output);
		
	//else if inputPopulation (input) already sorted by end_times of activity of selectedPlans:
//		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(persons, output); 
		
		routesWriter.writeFile();
			
	}

}

