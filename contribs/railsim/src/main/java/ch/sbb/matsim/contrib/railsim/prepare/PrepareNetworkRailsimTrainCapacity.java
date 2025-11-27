package ch.sbb.matsim.contrib.railsim.prepare;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

@CommandLine.Command(
	name = "prepare-network-railsim-capacity",
	description = "Utility class to set network capacity for railsim."
)
public class PrepareNetworkRailsimTrainCapacity implements MATSimAppCommand {


	@CommandLine.Option(names = "--input", required = true, description = "Input network file")
	private String input;

	@CommandLine.Option(names = "--output", required = true, description = "Output network file")
	private String output;

	@CommandLine.Option(names = "--capacity", required = true, description = "Capacity")
	private int capacity;

	public static void main(String[] args) {
		new PrepareNetworkRailsimTrainCapacity().execute(args);
	}

	@Override
	public Integer call() {

		Network network = NetworkUtils.readNetwork(input);
		for (Link link : network.getLinks().values()) {
			RailsimUtils.setTrainCapacity(link, capacity);
		}

		NetworkUtils.writeNetwork(network, output);

		return 0;
	}
}
