package org.matsim.application.prepare.network;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Set;

@CommandLine.Command(
	name = "clean-network",
	description = "Ensures that all links in the network are strongly connected."
)
public class CleanNetwork implements MATSimAppCommand {

	@CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Path to input network")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Output path", required = true)
	private Path output;

	@CommandLine.Option(names = "--modes", description = "List of modes to clean", split = ",", defaultValue = TransportMode.car)
	private Set<String> modes;

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(input.toString());

		var cleaner = new MultimodalNetworkCleaner(network);

		for (String m : modes) {
			cleaner.run(Set.of(m));
		}

		NetworkUtils.writeNetwork(network, output.toString());

		return 0;
	}
}
