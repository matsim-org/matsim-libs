package playground.balac.twowaycarsharing.utils;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;


public class CalculateCarSharingLegs {

	
	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath, String outputFilePath) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		int countCS = 0;
		int countC = 0;
		int countW = 0;
		int countB = 0;
		int countPT = 0;
		int count = 0;
		for (Person p: scenario.getPopulation().getPersons().values()) {
			
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if ((((Leg) pe).getTravelTime() > 0.0) && (((Leg) pe).getMode().equals( "car" ))) {
						countC++;
						count++;
					}
					if ((((Leg) pe).getTravelTime() > 0.0) && (((Leg) pe).getMode().equals( "carsharing" ))) {
						countCS++;
						count++;
					}
					if ((((Leg) pe).getTravelTime() > 0.0) && (((Leg) pe).getMode().equals( "bike" ))) {
						countB++;
						count++;
					}
					if ((((Leg) pe).getTravelTime() > 0.0) && (((Leg) pe).getMode().equals( "walk" ))) {
						countW++;
						count++;
					}
					if ((((Leg) pe).getTravelTime() > 0.0) && (((Leg) pe).getMode().equals( "pt" ))) {
						countPT++;
						count++;
					}
				}
			}

			
		}
		System.out.println(countCS);
		System.out.println(countC);
		System.out.println(countW);
		System.out.println(countB);
		System.out.println(countPT);
		System.out.println(count);
		System.out.println((double)countCS/count);
		
		//new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outputFilePath + "/plans_1p.xml");		
		
	}
	
	public static void main(String[] args) {
		
		CalculateCarSharingLegs cp = new CalculateCarSharingLegs();
				
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		String outputFolder = args[3];
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath, outputFolder);
	}

}
