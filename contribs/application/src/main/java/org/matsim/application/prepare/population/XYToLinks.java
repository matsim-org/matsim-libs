package org.matsim.application.prepare.population;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Set;


@CommandLine.Command(name = "xy-to-links", description = "Set link for activities based on coordinate", showDefaultValues = true)
public class XYToLinks implements MATSimAppCommand {

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;
	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;
	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private Path networkPath;

	@CommandLine.Option(names = "--car-only", description = "Convert to car-only network", defaultValue = "false")
	private boolean carOnly;

	@CommandLine.Option(names = "--facilities", description = "Input facilities. Necessary if activities already were assigned facility ids.")
	private Path inputFacilities;

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(networkPath.toString());

		if (carOnly) {
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);

			Network carOnlyNetwork = NetworkUtils.createNetwork();
			filter.filter(carOnlyNetwork, Set.of(TransportMode.car));
			network = carOnlyNetwork;
		}

		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

		if (inputFacilities != null) {
			Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
			MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
			reader.parse(inputFacilities.toUri().toURL());
			facilities = scenario.getActivityFacilities();
		}

		XY2Links algo = new XY2Links(network, facilities);

		Population population = PopulationUtils.readPopulation(input.toString());

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), algo);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

}
