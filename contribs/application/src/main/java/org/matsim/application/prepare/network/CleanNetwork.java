package org.matsim.application.prepare.network;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
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

	@CommandLine.Option(names = "--remove-turn-restrictions", description = "Remove turn restrictions for specified modes.", defaultValue = "false")
	private boolean rmTurnRestrictions;

	public static void main(String[] args) {
		new CleanNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(input.toString());

		if (rmTurnRestrictions) {
			for (Link link : network.getLinks().values()) {
				DisallowedNextLinks disallowed = NetworkUtils.getDisallowedNextLinks(link);
				if (disallowed != null) {
					modes.forEach(disallowed::removeDisallowedLinkSequences);
					if (disallowed.isEmpty()) {
						NetworkUtils.removeDisallowedNextLinks(link);
					}
				}
			}
		}

		var cleaner = new MultimodalNetworkCleaner(network);

		for (String m : modes) {
			cleaner.run(Set.of(m));
		}

		NetworkUtils.writeNetwork(network, output.toString());

		return 0;
	}
}
