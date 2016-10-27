package playground.santiago.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class WriteEndTimesFromPopulation {
	
	
	private static String plansFolder = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/plans/2_10pct/" ;
	private static String plansFile = plansFolder + "expanded_plans_0.xml" ;
	private static String outputFolder = "../../../shared-svn/projects/santiago/scenario/trips/";
	private final static Logger log = Logger.getLogger(WriteEndTimesFromPopulation.class);


	public static void main (String[]arg){

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(scenario);
		pr.readFile(plansFile);
		Population population = scenario.getPopulation();
		List<Person> persons = new ArrayList<>(population.getPersons().values());



		try {
			
			PrintWriter pw = new PrintWriter (new FileWriter ( outputFolder + "tripsMATSim.txt" ));

			for (Person p : persons){	


				for (Plan plan : p.getPlans()){				 
					List<PlanElement> pes = plan.getPlanElements();				 
					for ( PlanElement pe : pes){
						if (pe instanceof Activity){
							Activity actIn = (Activity)pe;							 

							pw.print(p.getId().toString() + ";" + actIn.getEndTime());						 
							if (actIn.getEndTime() == Double.NEGATIVE_INFINITY && !actIn.getType().equals("pt interaction")){pw.println("; ;");}
						}else{


							pw.println(";" + ((Leg) pe).getMode());
						}
					}
				}			 	 
			}

			pw.close();


		} catch (IOException e) {
			log.error(new Exception(e));
		}



	}

}
