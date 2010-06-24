package playground.mmoyo.Validators;

import java.io.File;
import java.util.Map.Entry;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;

public class PlanValidator {
	
	public PlanValidator(){
	
	}
	
	/** validates that initial and last activity of a plan are the same */
	private void validatePlan(String populationFilePath){
		final String WARNING = "Different type of initial and last activity of agent: ";
		ScenarioImpl scenario = new ScenarioImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(populationFilePath);
		Population population = scenario.getPopulation();

		int dif =0;
		for(Entry<Id, ? extends Person> entry: population.getPersons().entrySet() ){
			Id id = entry.getKey();
			PersonImpl value = (PersonImpl)entry.getValue();
			
			for (Plan plan: value.getPlans()){
				Activity firstAct = (Activity) plan.getPlanElements().get(0);
				Activity lastAct = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
				if (!firstAct.getType().equals(lastAct.getType())){
					System.out.println(WARNING + id );
					dif++;
				}
			}
		}
		double size = population.getPersons().size();
		double ddif = dif;
		System.out.println("total plans: " + population.getPersons().size() + " discrepancies: "+ dif +   " percentaje: " + (ddif/size)*100 );
	}
	
	private void validateMany(final String directoryPath){
		File directory = new File(directoryPath);
		final String SEPARATOR = "/";
		if (directory.exists()){
			for (String planFileName: directory.list()){    
				File file = new File (directoryPath + SEPARATOR + planFileName);
				if (file.isFile()){
					System.out.println (file.getPath());
					validatePlan(file.getPath());  //-> validate first that this is a plan file	
				}
				
			}
		}
	}
	
	/**validates all plans inside a given directory*/
	public static void main(String[] args) {
		//->  attention: distinguish automatically between directory and plan file: MatsimFileTypeGuesser
		
		//validate many files in a directory
		/*
		String directoryPath = null;
		if (args.length==1){
			directoryPath = args[0];
		}else{
			directoryPath = "../playgrounds/mmoyo/output/eightth/output";
		}
		new PlanValidator().validateMany (directoryPath);
		*/

		//validate one file
		String planFile = null;
		if (args.length==1){
			planFile = args[0];
		}else{
			planFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/baseplan_10x_subset_xy2links.xml.gz";
		}
		new PlanValidator().validatePlan(planFile) ;

	}

}
