package playground.balac.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class bla {

	public static void main(String[] args) throws IOException {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Documents/Projects/Claude_2015/MainSurvey/Dataset13/travelTimeCar.txt");

		
		for(Person per: sc.getPopulation().getPersons().values()) {
			double time1 = 0.0;
			double routeDistance1 = 0.0;
			double time2 = 0.0;
			double routeDistance2 = 0.0;
			Plan p = per.getPlans().get(0);
			
			boolean ind = false;
			for(PlanElement pe: p.getPlanElements()) {
				
				if (pe instanceof Leg) {
					if (!ind) {
						time1 += ((Leg) pe).getTravelTime();
										
						routeDistance1 += ((LinkNetworkRouteImpl) ((Leg) pe).getRoute()).getDistance();
						
						ind = true;
					}
					else {
						time2 += ((Leg) pe).getTravelTime();
						
						routeDistance2 += ((LinkNetworkRouteImpl) ((Leg) pe).getRoute()).getDistance();
						
					}
				}
				
			}
			
			outLink.write(per.getId() + ";");
			if (time1 < time2 + 300) {
				outLink.write(Double.toString(time1) + ";");
				outLink.write(Double.toString(routeDistance1) + ";");
			}
			else {
				outLink.write(Double.toString(time2) + ";");
				outLink.write(Double.toString(routeDistance2) + ";");
				
			}
		//	outLink.write(String.valueOf(CoordUtils.calcDistance(((Activity)p.getPlanElements().get(0)).getCoord(), sc.getNetwork().getLinks().get(linkId).getCoord())) + " ");
		//	outLink.write(String.valueOf(CoordUtils.calcDistance(a.getCoord(), sc.getNetwork().getLinks().get(linkId2).getCoord())));
			outLink.newLine();
			
			
		}
		outLink.flush();
		outLink.close();

	}

}
