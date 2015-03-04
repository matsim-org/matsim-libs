package playground.balac.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class bla1 {

	public static void main(String[] args) throws IOException {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		populationReader.readFile(args[0]);
		
final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/travelTimesPt_new.txt");

		
		for(Person per: sc.getPopulation().getPersons().values()) {
			double time = 0.0;
			Plan p = per.getPlans().get(0);
			int count = 0;
			double timet = 0.0;
			double timett = 0.0;
			boolean ind = false;
			boolean transit = false;
			for(PlanElement pe: p.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("leisure")) {
						
						break;
					}
					else {
						if (((Activity) pe).getType().equals("pt interaction") && ind && transit) {
							timett+=timet;
							
						}
						else if (((Activity) pe).getType().equals("pt interaction") && !ind) {
							ind = true;
						}
						
						
						
					}
				}
				else if (pe instanceof Leg) {
					
					if (((Leg) pe).getMode().equals("transit_walk")) {
						transit = true;
						count++;
						timet = ((Leg) pe).getTravelTime();
					}
					else transit = false;
					
					time += ((Leg) pe).getTravelTime();
					
				}
				
			}
			
			outLink.write(per.getId() + " ");
			outLink.write(Double.toString(time) + " ");
			outLink.write(Integer.toString(count - 2) + " ");
			outLink.write(Double.toString(timett));
			outLink.newLine();
			
			
		}
		outLink.flush();
		outLink.close();

	}

}
