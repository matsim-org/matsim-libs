package NetworkCreator;

import java.io.PrintWriter;
import java.util.List;
// Only works when the
public class Main {

	public static void main(String[] args) throws Exception {
		// Example list of slices for each square
		// Only works when the no. of slices increases
		List<Integer> slicesList = List.of(27,26, 21, 20, 19, 9, 8 ,7);
		MultiSquareCoordinateCalculator calculator = new MultiSquareCoordinateCalculator(slicesList);
		LinksCalculator linkCalculator = new LinksCalculator();

		List<String> nodes = calculator.getIntersections();
		List<String> links = linkCalculator.getLinks(calculator);

		// Start writing XML to a file.
		PrintWriter writer = new PrintWriter("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\contribs\\SyntheticModel\\test\\output.xml", "UTF-8");
		writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		writer.println("<!DOCTYPE network SYSTEM \"http://matsim.org/files/dtd/network_v1.dtd\">");
		writer.println();
		writer.println("<network name=\"test network for transit tutorial\">");
		writer.println("\t");
		writer.println("<nodes>");

		// Write nodes to the file.
		for (String node : nodes) {
			writer.println("\t" + node);
		}

		writer.println("</nodes>");
		writer.println("\t");
		writer.println("<links capperiod=\"1:00:00\">");

		// Write links to the file.
		for (String link : links) {
			writer.println("\t" + link);
		}

		writer.println("</links>");
		writer.println("\t");
		writer.println("</network>");
		writer.close();
	}
}
