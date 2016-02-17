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
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class TravelDistanceGrocery {
	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	PopulationReader populationReader = new MatsimPopulationReader(scenario);
	MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
	public void run(String plansFilePath, String networkFilePath) throws IOException {
		populationReader.readFile(plansFilePath);
		networkReader.readFile(networkFilePath);
		double distanceCar = 0.0;
	//	int countC = 0;
		double distanceBike = 0.0;
	//	int countB = 0;
		double distanceWalk = 0.0;
	//	int countW = 0;
		double distancePt = 0.0;
	//	int countPt = 0;
	//	int count = 0;
		Population pop = scenario.getPopulation();	
		for (Person p:pop.getPersons().values()) {
			Leg previousLeg = null;
			Activity previousActivity = null;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					previousLeg = (Leg) pe;
					
				}
				else if (pe instanceof Activity) {
					
					if (((Activity) pe).getType().startsWith( "work")) {
						if (previousLeg != null) {
						if (previousLeg.getMode().equals( "car" )) {
							distanceCar += RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute)previousLeg.getRoute(), scenario.getNetwork());
					//		countC++;
						}
						else if (previousLeg.getMode().equals( "bike" )) {
							distanceBike += CoordUtils.calcEuclideanDistance(previousActivity.getCoord(), ((Activity) pe).getCoord());
					//		countB++;
						}
						else if (previousLeg.getMode().equals( "walk" )) {
							distanceWalk += CoordUtils.calcEuclideanDistance(previousActivity.getCoord(), ((Activity) pe).getCoord());
						//	countW++;
						}
						else if (previousLeg.getMode().equals( "pt" )) {
							distancePt += CoordUtils.calcEuclideanDistance(previousActivity.getCoord(), ((Activity) pe).getCoord());
						//	countPt++;
						}
						}
					}
					previousActivity = (Activity) pe;
				}
				
			}
			
		}
		System.out.println((distanceCar+distanceBike+distanceWalk+distancePt)/scenario.getPopulation().getPersons().size());
		System.out.println(distanceCar/scenario.getPopulation().getPersons().size());
		System.out.println(distanceBike/scenario.getPopulation().getPersons().size());
		System.out.println(distanceWalk/scenario.getPopulation().getPersons().size());
		System.out.println(distancePt/scenario.getPopulation().getPersons().size());
		
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		TravelDistanceGrocery m = new TravelDistanceGrocery();
		m.run(args[0], args[1]);
	}
}
