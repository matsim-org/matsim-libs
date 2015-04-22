package playground.balac.avignon;

import java.io.IOException;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.TripAnalyzer;

public class ModalSplitGrocery {
	
	public void run(String[] input) throws IOException {
		double centerX = 683217.0; 
		double centerY = 247300.0;
		for (String plansFilePath : input) {
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

			PopulationReader populationReader = new MatsimPopulationReader(scenario);
			populationReader.readFile(plansFilePath);
	
			int count = 0;
			int countCar = 0;
			int countBike = 0;
			int countWalk = 0;
			int countPt = 0;
			
			Population pop = scenario.getPopulation();	
			for (Person p:pop.getPersons().values()) {
				Leg previousLeg = null;
				boolean act = true;
				boolean previousactivity = false;
				boolean lastactivity = false;
				boolean ptint = false;
				for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					
					if (pe instanceof Leg) {
						previousLeg = (Leg) pe;
						
						
					}
					else if (pe instanceof Activity ) {
							if (!((Activity) pe).getType().equals("pt interaction"))
								act = true;
							else
								ptint = true;
							
						
							previousactivity = lastactivity;
							if (!((Activity) pe).getType().equals("pt interaction")){
							if (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - centerX, 2) +(Math.pow(((Activity) pe).getCoord().getY() - centerY, 2))) < 30000) 
								lastactivity = true;
							else
								lastactivity = false;
							
							if (previousactivity && lastactivity) {
								if (previousLeg !=null) {
							if (previousLeg.getMode().equals( "car" )) {
								countCar++;
								count++;
							}
							else if (previousLeg.getMode().equals("bike")) {
								countBike++;
								count++;
							}
							else if (ptint && act) {
								ptint = false;
								act = false;
								countPt++;
								count++;
							}
							else  {
								countWalk++;
								count++;
								}

								}
							}
					}
							}
					
				}
				
			}
			System.out.println(count);
			System.out.println((double)countCar/(double)count);
			System.out.println((double)countBike/(double)count);
			System.out.println((double)countWalk/(double)count);
			System.out.println((double)countPt/(double)count);
		}
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		ModalSplitGrocery b = new ModalSplitGrocery();
		b.run(args);
		
		
		/*ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);

		TripAnalyzer tripAnalyzer = new TripAnalyzer(scenario.getNetwork());
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
	    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		events.addHandler(tripAnalyzer);
    	reader.parse(args[1]);
		
    	System.out.println(tripAnalyzer.createResults(null, 1));*/
	
	}
	
	
}
