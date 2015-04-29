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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class TravelTimeRealPT {
	
	public void run(String input, String attributes)  {
		double centerX = 683217.0; 
		double centerY = 247300.0;	
ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(attributes);	
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

			PopulationReader populationReader = new MatsimPopulationReader(scenario);
			MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		populationReader.readFile(input);
	//	networkReader.readFile(networkFilePath);
		double travelTimeCar = 0.0;
		int countC = 0;
		double travelTimeBike = 0.0;
		int countB = 0;
		double travelTimeWalk = 0.0;
		int countW = 0;
		double travelTimePt = 0.0;
		int countPt = 0;
		int count = 0;
		Population pop = scenario.getPopulation();	
		for (Person p:pop.getPersons().values()) {
			Leg previousLeg = null;
			boolean pt = false;
			double tempTTPT = 0.0;
			boolean previousactivity = false;
			boolean lastactivity = false;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					previousLeg = (Leg) pe;
					if (previousLeg.getMode().equals("pt") || previousLeg.getMode().equals("transit_walk"))
						tempTTPT += previousLeg.getTravelTime();
					
				}
				else if (pe instanceof Activity) {
					previousactivity = lastactivity;
					if (!((Activity) pe).getType().equals("pt interaction"))
					if (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - centerX, 2) +(Math.pow(((Activity) pe).getCoord().getY() - centerY, 2))) < 30000) 
						lastactivity = true;
					else
						lastactivity = false;
					
					
					
					if (((Activity) pe).getType().equals("pt interaction"))
						pt = true;
					else {
					
					if (previousactivity && lastactivity) {
						if (previousLeg !=null) {
							if (previousLeg.getMode().equals( "car" )) {
								travelTimeCar += previousLeg.getTravelTime();
								countC++;
								count++;
							}
							else if (previousLeg.getMode().equals( "bike" )) {
								travelTimeBike += previousLeg.getTravelTime();
								countB++;
								count++;
							}
							
							else if (pt) {
								travelTimePt += tempTTPT;
								tempTTPT = 0.0;
								countPt++;
								count++;
								pt = false;
							}
							else {
								travelTimeWalk += previousLeg.getTravelTime();
								countW++;
								count++;
							}
						}
					}
						
					}
				}
				
			}
			
		}
		System.out.println((travelTimeCar + travelTimeBike + travelTimeWalk + travelTimePt)/ count);
		System.out.println(travelTimeCar/(double) countC);
		System.out.println(travelTimeBike/(double) countB);
		System.out.println(travelTimeWalk/(double) countW);
		System.out.println(travelTimePt/(double) countPt);
		
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		TravelTimeRealPT m = new TravelTimeRealPT();
		//m.run(args);
	}
}
