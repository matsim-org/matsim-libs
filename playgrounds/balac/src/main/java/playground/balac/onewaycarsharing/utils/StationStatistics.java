package playground.balac.onewaycarsharing.utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.Id;

import playground.balac.onewaycarsharing.router.CarSharingStation;
import playground.balac.onewaycarsharing.router.CarSharingStations;

public class StationStatistics {

	public void run(String plansFilePath, String networkFilePath, String filename) throws FileNotFoundException, IOException {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		populationReader.readFile(plansFilePath);
		
		HashMap<Id, Integer> taken = new HashMap<Id, Integer>();
		HashMap<Id, Integer> returned = new HashMap<Id, Integer>();
		
		CarSharingStations css = new CarSharingStations(scenario.getNetwork());
		
		css.readFile(filename);
		
		for (Person p: scenario.getPopulation().getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Leg previousLeg = null;
			for(PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (((Activity) pe).getType() == "carsharingInteraction") {
						if (previousLeg.getMode() == "carsharing") {
							
							if (returned.get(((Activity) pe).getLinkId()) == null) {
								returned.put(((Activity) pe).getLinkId(), 1);
							}
							else {
								returned.put(((Activity) pe).getLinkId(), returned.get(((Activity) pe).getLinkId()) +1);
							}
						}
						else {
							if (taken.get(((Activity) pe).getLinkId()) == null) {
								taken.put(((Activity) pe).getLinkId(), 1);
							}
							else {
								taken.put(((Activity) pe).getLinkId(), taken.get(((Activity) pe).getLinkId()) +1);
							}
							
						}
						
						
					}
					
				}
				else {
					previousLeg = (Leg)pe;
				}
				
			}
			
		}
		  FileWriter fw = null;
		   BufferedWriter out = null;
		 
		   
		      fw = new FileWriter("C:/Users/balacm/Desktop/CarSharing/CSStatistics.txt");
		      System.out.println("");
		 out = new BufferedWriter(fw);
		 for (CarSharingStation cs : css.getStations().values()) {
			 out.write(cs.getId().toString()+ "\t" + cs.getCars() + "\t" + taken.get(cs.getLinkId()) + "\t" + returned.get(cs.getLinkId()));
			 out.write("\n");
			 out.flush();
		 }
		 out.close();
		 
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		StationStatistics ss = new StationStatistics();
		ss.run(args[0], args[1], args[2]);
		
		
	}

}
