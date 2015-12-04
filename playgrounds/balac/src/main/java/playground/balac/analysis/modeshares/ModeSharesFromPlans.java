package playground.balac.analysis.modeshares;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class ModeSharesFromPlans {

	public static void main(String[] args) {
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		int[] countModes = new int[4];
		
		double coordX = 683217.0;
		double coordY = 247300.0;	
		
		int count = 0;
		
		for (Person p: sc.getPopulation().getPersons().values()) {
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					if (!((Leg) pe).getMode().endsWith("carsharing")) {
					Route route =  ((Leg) pe).getRoute();
					Id<Link> linkId = route.getStartLinkId();
					Coord start_coord = sc.getNetwork().getLinks().get(((Leg) pe).getRoute().getStartLinkId()).getCoord();
					Coord end_coord = sc.getNetwork().getLinks().get(((Leg) pe).getRoute().getEndLinkId()).getCoord();

					boolean inside = false;
					if (Math.sqrt(Math.pow(start_coord.getX() - coordX, 2) +(Math.pow(start_coord.getY() - coordY, 2))) < 30000 
							&& Math.sqrt(Math.pow(end_coord.getX() - coordX, 2) +(Math.pow(end_coord.getY() - coordY, 2))) < 30000) {
						
						inside = true;
					}
					
					if (inside) {
						
					if (((Leg) pe).getMode().equals("car")) {
						
						countModes[0]++;
						count++;
						
					}
					else if (((Leg) pe).getMode().equals("walk")) {
						
						countModes[1]++;
						count++;

					}
					else if (((Leg) pe).getMode().equals("pt")) {
						
						countModes[2]++;
						count++;

					}
					else if (((Leg) pe).getMode().equals("bike")) {
						
						countModes[3]++;
						count++;

					}
					}
					}
				}
			}
		}
		
		for (int i = 0; i < 4; i++) {
			System.out.println((double)countModes[i]/(double)count);
		}
		

	}

}
