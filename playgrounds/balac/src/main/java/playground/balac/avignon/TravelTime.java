package playground.balac.avignon;

import java.io.IOException;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class TravelTime {
	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	PopulationReader populationReader = new MatsimPopulationReader(scenario);
	MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
	public void run(String plansFilePath, String networkFilePath) throws IOException {
		populationReader.readFile(plansFilePath);
		networkReader.readFile(networkFilePath);
		double distanceCar = 0.0;
		int countC = 0;
		double distanceBike = 0.0;
		int countB = 0;
		double distanceWalk = 0.0;
		int countW = 0;
		double distancePt = 0.0;
		int countPt = 0;
		int count = 0;
		Population pop = scenario.getPopulation();	
		for (Person p:pop.getPersons().values()) {
			Leg previousLeg = null;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					previousLeg = (Leg) pe;
					/*if (previousActivity.getType() == "shopgrocery") {
						if (previousLeg.getMode() == "car") {
							distanceCar += previousLeg.getTravelTime();
							countC++;
							count++;
						}
						else if (previousLeg.getMode() == "bike") {
							distanceBike += previousLeg.getTravelTime();
							countB++;
							count++;
						}
						else if (previousLeg.getMode() == "walk") {
							distanceWalk += previousLeg.getTravelTime();
							countW++;
							count++;
						}
						else if (previousLeg.getMode() == "pt") {
							distancePt += previousLeg.getTravelTime();
							countPt++;
							count++;
						}
					}*/
					
				}
				else if (pe instanceof Activity) {
					
					//if (((Activity) pe).getType().equals( "shopgrocery" )) {
					if (previousLeg !=null) {
						if (previousLeg.getMode().equals( "car" )) {
							distanceCar += previousLeg.getTravelTime();
							countC++;
							count++;
						}
						else if (previousLeg.getMode().equals( "bike" )) {
							distanceBike += previousLeg.getTravelTime();
							countB++;
							count++;
						}
						else if (previousLeg.getMode().equals( "walk" )) {
							distanceWalk += previousLeg.getTravelTime();
							countW++;
							count++;
						}
						else if (previousLeg.getMode().equals( "pt" )) {
							distancePt += previousLeg.getTravelTime();
							countPt++;
							count++;
						}
						
					}
				}
				
			}
			
		}
		System.out.println((distanceCar + distanceBike + distanceWalk + distancePt)/ count);
		System.out.println(distanceCar/(double) countC);
		System.out.println(distanceBike/(double) countB);
		System.out.println(distanceWalk/(double) countW);
		System.out.println(distancePt/(double) countPt);
		
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		TravelTime m = new TravelTime();
		m.run(args[0], args[1]);
	}
}
