package playground.balac.analysis.modeshares;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class ModeSharesPerPurpose {
	
	public static void main(String[] args) {
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		int[] shop = new int[4];
		int[] edu = new int[4];
		int[] leis = new int[4];
		int[] work = new int[4];

		
		for (Person p : sc.getPopulation().getPersons().values()) {
			
			Plan plan = p.getSelectedPlan();
			Leg previousLeg = null;
			for (PlanElement pe : plan.getPlanElements()) {
				
				if (pe instanceof Leg)
					previousLeg = (Leg) pe;
				else if (pe instanceof Activity) {
					
					if (((Activity) pe).getType().equals("shop")) {
						
						if (previousLeg.getMode().equals("car"))
							shop[0]++;
						else if (previousLeg.getMode().equals("bike"))
							shop[1]++;
						else if (previousLeg.getMode().equals("walk"))
							shop[2]++;
						else if (previousLeg.getMode().equals("pt"))
							shop[3]++;
						
						
					}
					
					
				}
			}
			
		}
		
		int count = shop[0] + shop[1] + shop[2] + shop[3];
		
		System.out.println((double)shop[0]/(double)count);
		System.out.println((double)shop[1]/(double)count);
		System.out.println((double)shop[2]/(double)count);
		System.out.println((double)shop[3]/(double)count);

		
	}
	

}
