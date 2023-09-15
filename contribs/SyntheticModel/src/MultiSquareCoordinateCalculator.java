import java.util.ArrayList;
import java.util.List;

public class MultiSquareCoordinateCalculator extends IntersectionCoordinateCalculator {

	private List<Integer> slicesPerSquare;

	public MultiSquareCoordinateCalculator(List<Integer> slicesPerSquare) {
		// You can use any default value here since we'll be overriding the method completely.
		super(1);
		this.slicesPerSquare = slicesPerSquare;
	}

	@Override
	public List<String> getIntersections() {
		List<String> nodes = new ArrayList<>();
		int currentXOrigin = 0;

		for (Integer slices : slicesPerSquare) {
			// Calculate the increment for the current square.
			double increment = 1000.0 / slices;

			for (int i = 0; i <= slices; i++) {
				for (int j = 0; j <= slices; j++) {
					double x = currentXOrigin + (i * increment);
					double y = j * increment;
					String nodeId = String.format("%d0%02d%02d",(int)((x / 1000)+1), (int)(x/ increment), (int)(y / increment));
					nodes.add(String.format("<node id=\"%s\" x=\"%.2f\" y=\"%.2f\" />", nodeId, x, y));
				}
			}

			// Move to the next square in the x-axis.
			currentXOrigin += 1000;
		}

		return nodes;
	}

	public static void main(String[] args) {
		List<Integer> slicesList = List.of(3, 4);  // Example list
		MultiSquareCoordinateCalculator calculator = new MultiSquareCoordinateCalculator(slicesList);
		List<String> intersections = calculator.getIntersections();
		for (String intersection : intersections) {
			System.out.println(intersection);
		}
	}
}

