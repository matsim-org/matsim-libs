package playground.christoph.population;

import java.io.IOException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;

/*
 * Dumps all plans of the Plansfile except the selected one.
 * Additionally the score could be reseted.
 */
public class PlansDumping {

	private static String configFileName = "../matsim/mysimulations/kt-zurich/config.xml";
	private static String networkFile = "../matsim/mysimulations/kt-zurich/input/network.xml";
	private static String populationFile = "../matsim/mysimulations/kt-zurich/input/plans.xml.gz";
	private static String populationOutFile = "../matsim/mysimulations/kt-zurich/input/out_plans.xml.gz";
	private static final String dtdFileName = null;

	private static final String separator = System.getProperty("file.separator");

	public static void main(String[] args)
	{
		configFileName = configFileName.replace("/", separator);
		networkFile = networkFile.replace("/", separator);
		populationFile = populationFile.replace("/", separator);

		Config config = new Config();
		config.addCoreModules();
		try
		{
			new MatsimConfigReader(config).readFile(configFileName, dtdFileName);
		}
		catch (IOException e)
		{
			System.out.println("Problem loading the configuration file from " + configFileName);
			throw new RuntimeException(e);
		}
		Gbl.setConfig(config);
		ScenarioImpl scenario = new ScenarioImpl(config);

		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFile);

		for (Person person : population.getPersons().values())
		{
			((PersonImpl) person).removeUnselectedPlans();
			person.getSelectedPlan().setScore(null);
		}

		new PopulationWriter(population, network).write(populationOutFile);
		System.out.println("Done");
	}

}
