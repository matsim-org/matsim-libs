package playground.christoph.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


/*
 * Dumps LinkIds out of all plans.
 */
public class PlansLinkReferenceDumping {

	private static String configFileName = "../../matsim/mysimulations/multimodal/config.xml";
	private static String networkFile = "../../matsim/mysimulations/multimodal/input/network.xml.gz";
	private static String facilitiesFile = "../../matsim/mysimulations/multimodal/input/facilities_KTIYear2.xml.gz";
	private static String populationFile = "../../matsim/mysimulations/multimodal/input/plans.xml.gz";
	private static String populationOutFile = "../../matsim/mysimulations/multimodal/input/out_plans.xml.gz";
	private static final String dtdFileName = null;

	private static final String separator = System.getProperty("file.separator");

	public static void main(String[] args) {
		configFileName = configFileName.replace("/", separator);
		networkFile = networkFile.replace("/", separator);
		populationFile = populationFile.replace("/", separator);

		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFileName, dtdFileName);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);

		ActivityFacilities facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFile);
		
		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFile);

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						ActivityImpl activity = (ActivityImpl) planElement;
						activity.setLinkId(null);
					} else if (planElement instanceof Leg) {
						LegImpl leg = (LegImpl) planElement;
						leg.setRoute(null);
					}
				}
			}
		}

		new PopulationWriter(population, network).write(populationOutFile);
		System.out.println("Done");
	}
}
