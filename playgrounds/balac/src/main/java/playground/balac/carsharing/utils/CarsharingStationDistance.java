package playground.balac.carsharing.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class CarsharingStationDistance {
	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath, String outputFilePath) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		
		int count = 0;
		int count1 = 0;
		int[] bla = new int[30];
		
		for (Person p: scenario.getPopulation().getPersons().values()) {			
			
			Activity a = null;
			Leg leg = null;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					
				if (pe instanceof Activity) 
					if (((Activity) pe).getType().equals( "carsharingInteraction" ) && leg.getMode().equals( "carsharingwalk" )) {
						if (CoordUtils.calcEuclideanDistance(scenario.getActivityFacilities().getFacilities().get(a.getFacilityId()).getCoord(), scenario.getNetwork().getLinks().get(((Activity) pe).getLinkId()).getCoord()) < 2000) {
							bla[(int)CoordUtils.calcEuclideanDistance(scenario.getActivityFacilities().getFacilities().get(a.getFacilityId()).getCoord(), scenario.getNetwork().getLinks().get(((Activity) pe).getLinkId()).getCoord())/100]++;
							count++;
						}
						else count1++;
					}
					else
						a = (Activity) pe;
				else if (pe instanceof Leg) {
					
					leg = (Leg) pe;
				}
				
			}
		}
		
		for (int i = 0; i < bla.length; i++) 
			System.out.println((double)bla[i]/(double)count);
		System.out.println(count);
		System.out.println(count1);	
		}
		
	public static void main(String[] args) {
		CarsharingStationDistance cp = new CarsharingStationDistance();
		
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		String outputFolder = args[3];
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath, outputFolder);

	}

}
