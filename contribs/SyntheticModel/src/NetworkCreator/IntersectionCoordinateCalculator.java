package NetworkCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;
import java.util.List;

public class IntersectionCoordinateCalculator {

	private final int Meshsize = 1000; // square Meshsize
	private final int slices;

	public IntersectionCoordinateCalculator(int slices) {
		if (slices < 1) {
			throw new IllegalArgumentException("Number of slices must be at least 1.");
		}
		this.slices = slices + 2;
	}

	public List<Node> getIntersections() {
		List<Node> intersections = new ArrayList<>();
		double increment = (double) Meshsize / (slices - 1);

		for (int i = 0; i < slices; i++) {
			for (int j = 0; j < slices; j++) {
				double x = increment * i;
				double y = increment * j;
				String nodeIdStr = String.format("%02d%02d", i + 1, j + 1);
				Id<Node> nodeId = Id.createNodeId(nodeIdStr);
				Coord coord = new Coord(x, y);
				Node node = NetworkUtils.createNode(nodeId, coord);
				intersections.add(node);
			}
		}

		return intersections;
	}
}
