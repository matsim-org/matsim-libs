
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

	public List<String> getIntersections() {
		List<String> intersections = new ArrayList<>();
		double increment = (double) Meshsize / (slices - 1);

		for (int i = 0; i < slices; i++) { // We skip the 0 and 1000 marks as they are edges, not intersections
			for (int j = 0; j < slices; j++) {
				double x = increment * i;
				double y = increment * j;
				String nodeId = String.format("%02d%02d", i + 1, j + 1);  // Formatting i and j to be two digits
				String node = String.format("<node id=\"%s\" x=\"%.2f\" y=\"%.2f\" />", nodeId, x, y);
				intersections.add(node);
			}
		}

		return intersections;
	}
}
