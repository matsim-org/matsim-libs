package playground.santiago.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;




public class ModifyAgentAttributes {

	final static String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/";
	
	final static String expandedPlansFolder = svnWorkingDir + "plans/2_10pct/";
	final static String expandedPlansFile = expandedPlansFolder + "randomized_expanded_plans.xml.gz";

	final static String sampledPlansFolder = svnWorkingDir + "plans/3_1pct/";
	final static String sampledPlansFile = sampledPlansFolder + "randomized_sampled_plans.xml.gz";
	

	final static String expandedAgentAttributes = expandedPlansFolder + "expandedAgentAttributes.xml";
	final static String sampledAgentAttributes = sampledPlansFolder + "sampledAgentAttributes.xml";
	final static String agentsWithCar  = svnWorkingDir + "plans/1_initial/workDaysOnly/agentsWithCar.txt";
	
	
	private final static Logger log = Logger.getLogger(ModifyAgentAttributes.class);
	
	public static void main(final String[] args) {

		LinkedList <String> carUsers = new LinkedList<>(); 

		
		try {	
			
			BufferedReader br = IOUtils.getBufferedReader(agentsWithCar);
			String line = br.readLine();
			while ((line = br.readLine()) != null) {				
				carUsers.add(line);	//example: 10508202			
			}
						
			br.close();
			
		} catch (IOException e) {
			
		log.error(new Exception(e));
		
		}

		Scenario expandedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(expandedScenario).readFile(expandedPlansFile);
		Population expandedPopulation = expandedScenario.getPopulation();
		writeExpandedAttributes(expandedPopulation, carUsers);
		
		
		
		
		Scenario sampledScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sampledScenario).readFile(sampledPlansFile);
		Population sampledPopulation = sampledScenario.getPopulation();
		writeSampledAttributes(sampledPopulation, carUsers);

	
		
		
		}
		
	private static void writeSampledAttributes(Population population, LinkedList<String> carUsers){
		
		
		LinkedList<Person> persons = new LinkedList<>(population.getPersons().values());		
		LinkedList<String> clonedIds = new LinkedList<>(); //example: 10508202_1
		LinkedList<String> originalIds = new LinkedList<>(); //example: 10508202
		
		for (Person p : persons) {
			clonedIds.add(p.getId().toString());			
			String [] keyId = p.getId().toString().split("_");
			originalIds.add( keyId[0] );
			
		}
		
		Collections.sort(originalIds);
		Collections.sort(clonedIds);
		
		
		
		//not necessary
		Collections.sort(carUsers);
		
		ObjectAttributes oa = new ObjectAttributes();
		
		for(String id : carUsers){
			
				int start = originalIds.indexOf(id);
				int end = originalIds.lastIndexOf(id);
	
				if (start!=-1){
					for (int i = start; i<=end; i++){					
					oa.putAttribute(clonedIds.get(i), "carUsers", "carAvail");					
					}
				}

			}
		
		
		new ObjectAttributesXmlWriter(oa).writeFile(sampledAgentAttributes);
		
		
		
		
	}	
	
	private static void writeExpandedAttributes(Population population, LinkedList<String> carUsers){
		
		LinkedList<Person> persons = new LinkedList<>(population.getPersons().values());		
		LinkedList<String> clonedIds = new LinkedList<>(); //example: 10508202_1
		LinkedList<String> originalIds = new LinkedList<>(); //example: 10508202
		
		for (Person p : persons) {
			clonedIds.add(p.getId().toString());			
			String [] keyId = p.getId().toString().split("_");
			originalIds.add( keyId[0] );
			
		}
		
		Collections.sort(originalIds);
		Collections.sort(clonedIds);
		
		
		
		//not necessary
		Collections.sort(carUsers);
		
		ObjectAttributes oa = new ObjectAttributes();
		
		for(String id : carUsers){
			
				int start = originalIds.indexOf(id);
				int end = originalIds.lastIndexOf(id);
	
				if (start!=-1){
					for (int i = start; i<=end; i++){					
					oa.putAttribute(clonedIds.get(i), "carUsers", "carAvail");					
					}
				}

			}
		
		
		new ObjectAttributesXmlWriter(oa).writeFile(expandedAgentAttributes);
		
		
	}
		
		
		
	}

