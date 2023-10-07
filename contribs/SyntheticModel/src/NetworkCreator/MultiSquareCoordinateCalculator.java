package NetworkCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;
import java.util.List;

public class MultiSquareCoordinateCalculator {

	private List<Integer> slicesPerSquare;

	public MultiSquareCoordinateCalculator(List<Integer> slicesPerSquare) {
		this.slicesPerSquare = slicesPerSquare;
	}

	public List<Node> getIntersections() {
		List<Node> nodes = new ArrayList<>();
		int currentXOrigin = 0;

		for (Integer slices : slicesPerSquare) {
			// Calculate the increment for the current square.
			double increment = 1000.0 / slices;

			for (int i = 0; i <= slices; i++) {
				for (int j = 0; j <= slices; j++) {
					double x = currentXOrigin + (i * increment);
					double y = j * increment;
					String nodeIdStr = String.format("%d0%02d%02d",(int)(x / 1000 +1), (int)(i), (int)(j));
					Id<Node> nodeId = Id.createNodeId(nodeIdStr);
					Coord coord = new Coord(x, y);
					Node node = NetworkUtils.createNode(nodeId, coord);
					nodes.add(node);
				}
			}

			// Move to the next square in the x-axis.
			currentXOrigin += 1000;
		}

		return nodes;
	}

	public static void main(String[] args) {
		List<Integer> slicesList = List.of(3, 6);  // Example list
		MultiSquareCoordinateCalculator calculator = new MultiSquareCoordinateCalculator(slicesList);
		List<Node> intersections = calculator.getIntersections();
		for (Node intersection : intersections) {
			System.out.println("Node ID: " + intersection.getId() + ", Coord: (" + intersection.getCoord().getX() + ", " + intersection.getCoord().getY() + ")");
		}
	}
}
