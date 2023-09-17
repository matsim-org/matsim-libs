package PlansCreator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomCoordinatesGenerator {

	private static final double SQUARE_SIZE = 1000.0;
	private final Random random;
	private final List<Integer> densities;
	private final List<Double> decayRates;  // For storing decay rates for each square

	public RandomCoordinatesGenerator(List<Integer> densities, List<Double> decayRates) {
		this.densities = densities;
		this.decayRates = decayRates;
		this.random = new Random();
	}

	public List<Coordinate> generateCoordinates() {
		List<Coordinate> coordinates = new ArrayList<>();

		for (int i = 0; i < densities.size(); i++) {
			int density = densities.get(i);
			double decayRate = decayRates.get(i);
			double centerX = i * SQUARE_SIZE + SQUARE_SIZE / 2;
			double centerY = SQUARE_SIZE / 2;

			int pointsWithinRadius = (int) (density * decayRate);

			// Generating points within the 400-length radius
			for (int j = 0; j < pointsWithinRadius; j++) {
				double theta = 2 * Math.PI * random.nextDouble();
				double r = 250 * Math.sqrt(random.nextDouble()); // sqrt for uniform point distribution inside a circle

				double x = centerX + r * Math.cos(theta);
				double y = centerY + r * Math.sin(theta);

				coordinates.add(new Coordinate(x, y));
			}

			// Generating points outside the 400-length radius uniformly in the square
			for (int j = pointsWithinRadius; j < density; j++) {
				double x = centerX + (random.nextDouble() - 0.5) * SQUARE_SIZE;
				double y = centerY + (random.nextDouble() - 0.5) * SQUARE_SIZE;

				coordinates.add(new Coordinate(x, y));
			}
		}

		return coordinates;
	}



	public void writeToXML(String filePath, String filename) throws IOException {
		List<Coordinate> coordinates = generateCoordinates();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath + "/" + filename))) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

			// Writing input parameters as a comment in the XML
			writer.write("<!-- Input parameters: \n");
			writer.write("\tDensities: " + densities.toString() + "\n");
			writer.write("\tDecay rates: " + decayRates.toString() + "\n");
			writer.write("-->\n");

			writer.write("<nodes>\n");

			for (Coordinate coord : coordinates) {
				String line = String.format("\t<node x=\"%.2f\" y=\"%.2f\" />\n", coord.x, coord.y);
				writer.write(line);
			}

			writer.write("</nodes>\n");
		}
	}

	public static class Coordinate {
		public final double x;
		public final double y;

		public Coordinate(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}

	public static void main(String[] args) {
		List<Integer> densities = List.of(8000, 10000, 4500, 4500, 4500, 700, 500, 700);
		List<Double> decayRates = List.of(0.6, 0.8, 0.7, 0.4, 0.3, 0.4, 0.1, 0.9 );  // Example decay rates for each square
		RandomCoordinatesGenerator generator = new RandomCoordinatesGenerator(densities, decayRates);

		try {
			generator.writeToXML("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine","households.xml");
			System.out.println("XML file has been written successfully!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
