package playground.balac.utils;

import java.io.BufferedWriter;
import java.io.IOException;

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
import org.matsim.core.utils.io.IOUtils;

public class PTDistanceFromPlans {

	public static void main(String[] args) throws IOException {
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter("P:/_TEMP/sschmutz/routing_matsim/20141009_trip_pt_timevar/output/pt_trip_vartime_distance_10.txt");

		
		for (Person p: sc.getPopulation().getPersons().values()) {
			double distance = 0.0;
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("pt"))
						distance += ((Leg) pe).getRoute().getDistance();
				}
				else if (pe instanceof Activity) {
					if (((Activity) pe).getType().endsWith("leisure"))
						break;
					
				}
			}
			
			outLink.write(p.getId() + " " + Double.toString(distance));
			outLink.newLine();
			
		}
		outLink.flush();
		outLink.close();

	}

}
