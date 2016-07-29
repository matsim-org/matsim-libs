package playground.balac.utils.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class UpdateInitialTravelTimes {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
			int index = 0;
			for (PlanElement pe : plan.getPlanElements()) {
				
				if (pe instanceof Leg) {
					
					String mode = ((Leg) pe).getMode();
					double distance = 1.3 * CoordUtils.calcEuclideanDistance(((Activity)plan.getPlanElements().get(index - 1)).getCoord(),
							((Activity)plan.getPlanElements().get(index + 1)).getCoord());
					
					if (mode.equals("walk")) {
						((Leg) pe).setTravelTime(distance / 1.05);
						((Leg) pe).getRoute().setTravelTime(distance / 1.05);
					}
					else if (mode.equals("pt")) {
						((Leg) pe).setTravelTime(distance / 2.18 * 4);
						((Leg) pe).getRoute().setTravelTime(distance / 2.18 * 4);

					}
					else if (mode.equals("bike")) {
						((Leg) pe).setTravelTime(distance / 1.9 * 4);
						((Leg) pe).getRoute().setTravelTime(distance / 1.9 * 4);

					}
				}
				index++;
			}
		}
		
		new PopulationWriter(scenario.getPopulation(),
				scenario.getNetwork()).writeV4("C:/Users/balacm/Documents/InducedDemand/plans_0.1perc_fixedtt_50perc_inc.xml.gz");		

		
	}

}
