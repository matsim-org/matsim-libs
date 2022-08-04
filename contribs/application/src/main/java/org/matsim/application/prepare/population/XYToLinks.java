package org.matsim.application.prepare.population;

import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.facilities.FacilitiesUtils;
import picocli.CommandLine;

import java.nio.file.Path;


@CommandLine.Command(name = "xy-to-links", description = "Set link for activities based on coordinate", showDefaultValues = true)
public class XYToLinks implements MATSimAppCommand {

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;
	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;
	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private Path networkPath;

	@Override
	public Integer call() throws Exception {

		XY2Links algo = new XY2Links(NetworkUtils.readNetwork(networkPath.toString()), FacilitiesUtils.createActivityFacilities());

		Population population = PopulationUtils.readPopulation(input.toString());

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), algo);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

}
