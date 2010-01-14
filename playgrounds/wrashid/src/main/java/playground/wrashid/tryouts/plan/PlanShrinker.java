package playground.wrashid.tryouts.plan;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;

public class PlanShrinker {

	public static void main(final String[] args) {

		// input plan defined in config file
		// output plan defined in last line
		// percentage defined in last line

		String outputPath="C:/data/workspaceYourKit6/matsim/output/";
		String configFile = outputPath +  "config.xml";

		Config config = Gbl.createConfig(new String[] {configFile});
		ScenarioImpl scenario = new ScenarioImpl();

		System.out.println("  reading the network...");
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		PopulationImpl population = scenario.getPopulation();
		population.setIsStreaming(true);

		System.out.println("reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();

		new PopulationWriter(population,network,0.1).writeFile(outputPath+"plans1.xml");

	}

}
