package playground.dhosse.cl.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class PopulationAnalysis {

	private static final String runPath = "../../runs-svn/santiago/run11b/output/";
	
	private static final String analysisPath = runPath+ "../analysis/";
	
	public static void main(String[] args) {
		
		createDir(new File(analysisPath));

		getPersonsWithNegativeScores();
		getPersonsWithCarLegWOCarAvail();
		
		System.out.println("### Done. ###");
		
	}
	
	private static void getPersonsWithNegativeScores(){
		
		String plansFile = runPath + "output_plans.xml.gz";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).parse(plansFile);
		
		Set<Person> plansWithNegativeScores = new HashSet<Person>();
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			if(person.getSelectedPlan().getScore() < 0){
				
				plansWithNegativeScores.add(person);
				
			}
			
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(analysisPath + "negativeScores.txt");
		
		try {
			
			for(Person person : plansWithNegativeScores){
				
				writer.write(person.getId().toString() + "\t" + person.getSelectedPlan().getScore());
				
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

	private static void getPersonsWithCarLegWOCarAvail(){
		
		String plansFile = runPath + "output_plans.xml.gz";
		String attributesFile = runPath + "output_personAttributes.xml.gz";
		
		Map<String, Boolean> agentIdString2CarAvail = new HashMap<String, Boolean>();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).parse(plansFile);
	
		//collect agents using car on their tour
		for(Person person : scenario.getPopulation().getPersons().values()){
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
					if (pe instanceof Leg){
						Leg leg = (Leg) pe;
						if (leg.getMode() == "car"){ 
							agentIdString2CarAvail.put(person.getId().toString(), false); // Assumption, that carAvail is false; carAvail will be checked later.
						}
					}
			}
		}
		System.out.println("Number of agents using car: " + agentIdString2CarAvail.size());
		
		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader attrReader = new ObjectAttributesXmlReader(attributes);
		attrReader.parse(attributesFile);
		
		for (String agentIdString : agentIdString2CarAvail.keySet()) {
			System.out.println(agentIdString + ": " +  attributes.getAttribute(agentIdString , "carAvail"));
			boolean carAvail = "carAvail".equals(attributes.getAttribute(agentIdString , "carAvail"));
			agentIdString2CarAvail.put(agentIdString, carAvail);
		}
		
		int countCarUserswihtCarAvail = 0;
		for (String agentIdString : agentIdString2CarAvail.keySet()) {
			if (agentIdString2CarAvail.get(agentIdString) == true) {
				countCarUserswihtCarAvail ++;
			}
		}
		System.out.println(countCarUserswihtCarAvail + " of " + agentIdString2CarAvail.size() + " have a car available.");
		
		BufferedWriter writer = IOUtils.getBufferedWriter(analysisPath + "carUseWOCarAvailable.txt");
		
		
		try {
			writer.write("There are " + (agentIdString2CarAvail.size() - countCarUserswihtCarAvail) + " agents using a car, but having no car available: ");
			writer.newLine();
			
			for (String agentIdString : agentIdString2CarAvail.keySet()) {
				if (agentIdString2CarAvail.get(agentIdString) == false) {
					writer.write(agentIdString);
					writer.newLine();
				}
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	private static void createDir(File file) {
		System.out.println("Directory " + file + " created: "+ file.mkdirs());	
	}
	
}