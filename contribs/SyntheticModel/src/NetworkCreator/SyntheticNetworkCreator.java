package NetworkCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.io.PrintWriter;
import java.util.List;
// Only works when the
public class SyntheticNetworkCreator {

	public static void main(String[] args) throws Exception {
		// Example list of slices for each square
		// Only works when the no. of slices increases
		List<Integer> slicesList = List.of(
			30, 27, 26, //main core
			21, 20, 20, 20, 20, 19, 19, 19, 18, 18, 18, 18, 18, 17, 17, 17, 17, //urban area
			16, 16, 15, 14, 14, 13, 13, 12, 12, 10, 9, 9, 9, 9, 9, //suburban transition
			8, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6 //suburb
		);

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

		// Write the network to an XML file using NetworkWriter
		new NetworkWriter(network).write("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\40kmx1km\\network40x1km.xml");
	}
}
