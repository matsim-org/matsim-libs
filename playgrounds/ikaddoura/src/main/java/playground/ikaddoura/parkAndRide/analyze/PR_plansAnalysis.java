package playground.ikaddoura.parkAndRide.analyze;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;

public class PR_plansAnalysis {

//	String netFile = "/Users/Ihab/ils4/kaddoura/parkAndRide/input/PRnetwork_berlin.xml";
//	String plansFile1 = "/Users/Ihab/Desktop/population1.xml";
//	String plansFile2 = "/Users/Ihab/Desktop/population2.xml";
//	String plansFile1 = "/Users/Ihab/ils4/kaddoura/parkAndRide/output/berlin_run4_transferPenalty-5/ITERS/it.0/0.plans.xml.gz";
//	String plansFile2 = "/Users/Ihab/ils4/kaddoura/parkAndRide/output/berlin_run4_transferPenalty-5/ITERS/it.20/20.plans.xml.gz";
	
	static String plansFile1;
	static String plansFile2;
	static String netFile;
	static String outputFile;
			
	public static void main(String[] args) throws IOException {
		
		plansFile1 = args[0];
		plansFile2 = args[1];
		netFile = args[2];
		outputFile = args[3];
		
		PR_plansAnalysis analyse = new PR_plansAnalysis();
		analyse.run();
	}
	
	public void run() {
		
		Population population1 = getPopulation(netFile, plansFile1);
		Population population2 = getPopulation(netFile, plansFile2);
		List <Id> personIds = new ArrayList<Id>();
		
		for (Person person1 : population1.getPersons().values()){
			for (Plan plan1 : person1.getPlans()){
				double score1 = plan1.getScore();
				Id personId1 = person1.getId();
				
				Person person2 = population2.getPersons().get(personId1);
				for (Plan plan2 : person2.getPlans()){
					boolean plan2HasPR = false;
					double score2 = plan2.getScore();
					if (score2 > score1) {
						for (PlanElement pE : plan2.getPlanElements()){
							if (pE instanceof Activity){
								Activity act = (Activity) pE;
								if (act.toString().contains("parkAndRide")){
									plan2HasPR = true;
								}
							}
						}
						if (plan2HasPR){
//							System.out.println(person2.getId() + " hat in planFile2 einen Park'n'Ride-Plan mit höherem Score.");
						} else {
							System.out.println(person2.getId() + " hat in planFile2 einen Plan mit höherem Score, der kein Park'n'Ride enthält!!!");
							System.out.println("Verbesserung nicht aufgrund von Park'n'Ride!");
							personIds.add(person2.getId());
						}
					}	
				}
			}
		}
		System.out.println("done.");
		
		TextFileWriter writer = new TextFileWriter();
		writer.writeFile(personIds, outputFile);
	}

//------------------------------------------------------------------------------------------------------------------------
	
	private Population getPopulation(String netFile, String plansFile){
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		return population;
	}

}
