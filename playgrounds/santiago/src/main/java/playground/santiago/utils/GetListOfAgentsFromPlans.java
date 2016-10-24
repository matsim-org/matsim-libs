package playground.santiago.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;



public class GetListOfAgentsFromPlans {
	
	 final String plansFolder = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/plans/1_initial/workDaysOnly/";	 
	 final String plansFile = "plans_final.xml.gz";
	 final String plans = plansFolder + plansFile;
	 final String outputFile = plansFolder + "agents.txt";
	 
	 
		private final static Logger log = Logger.getLogger(GetListOfAgentsFromPlans.class);

	 
	 public static void main (String[]arg){
		 GetListOfAgentsFromPlans afp = new GetListOfAgentsFromPlans();
		 afp.run();
	 }
	 
	 private void run () {
		 getList(plans);
	 }
	
	 private void getList(String plans){
		 
		 Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		 new PopulationReader(scenario).readFile(plans);		 
		 Population population = scenario.getPopulation();		 
		 List<Person> persons = new ArrayList<>(population.getPersons().values());
		 
		 
		 try {
		 PrintWriter pw = new PrintWriter (new FileWriter ( outputFile ));
		 
		 for (Person p : persons) {
				pw.println( p.getId().toString() );	
		 }
		 
		 pw.close();
		 
		 }catch(IOException e){
			 log.error(new Exception(e));
		 }
		 
		 
	
		 
		 
	 }
	 
	 
	 
}
