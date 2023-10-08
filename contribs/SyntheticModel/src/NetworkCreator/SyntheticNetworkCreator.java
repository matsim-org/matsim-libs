package NetworkCreator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class SyntheticNetworkCreator {

	private static final String NETWORK_FILENAME = "network.xml";

	public void createNetwork(String scenarioPath, List<Integer> slicesList) throws Exception {
		if (slicesList == null || slicesList.isEmpty()) {
			throw new IllegalArgumentException("Provided slicesList is empty or null.");
		}

		MultiSquareCoordinateCalculator calculator = new MultiSquareCoordinateCalculator(slicesList);
		LinksCalculator linkCalculator = new LinksCalculator();

		List<Node> nodes = calculator.getIntersections();
		List<Link> calculatedLinks = linkCalculator.getLinks(calculator);

		// Create a new MATSim network
		Network network = NetworkUtils.createNetwork();

		// Add nodes to the network
		for (Node node : nodes) {
			network.addNode(node);
		}

		// Add links to the network
		for (Link link : calculatedLinks) {
			network.addLink(link);
		}

		// Create the output path for the network
		String outputPath = scenarioPath + "/" + NETWORK_FILENAME;

		// Write the network to the output path using NetworkWriter
		new NetworkWriter(network).write(outputPath);
	}

	public static void main(String[] args, List<Integer> slicesList) throws Exception {
		// Checking if there's at least one argument (the scenarioPath)
		if(args.length < 1) {
			System.err.println("Please provide the scenario path as the first argument.");
			return;
		}

		String scenarioPath = args[0];

		// Extracting slicesList from arguments (starting from the second argument)
		for (int i = 1; i < args.length; i++) {
			slicesList.add(Integer.parseInt(args[i]));
		}

		if (slicesList.isEmpty()) {
			System.err.println("Please provide slices as additional arguments after the scenario path.");
			return;
		}

		new SyntheticNetworkCreator().createNetwork(scenarioPath, slicesList);
	}
}

