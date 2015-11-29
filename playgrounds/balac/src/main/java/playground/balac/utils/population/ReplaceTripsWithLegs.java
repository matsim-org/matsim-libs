package playground.balac.utils.population;

import java.util.Arrays;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.TripsToLegsAlgorithm;

public class ReplaceTripsWithLegs {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		final StageActivityTypes types =
				new StageActivityTypesImpl(
						Arrays.asList(
							"pt interaction"));

		final TripsToLegsAlgorithm algorithm = new TripsToLegsAlgorithm( types , new MainModeIdentifierImpl() );
				
		for(Person p : scenario.getPopulation().getPersons().values()) {
			
			algorithm.run( p.getSelectedPlan() );
			
			
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Activity) {
					
					if (((Activity) pe).getType().equals("home")) {
						
						((Activity) pe).getCoord().getX();
					}
				}
			}

		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(args[2] + "/plans_1perc_legs.xml.gz");		
		
	}
}
