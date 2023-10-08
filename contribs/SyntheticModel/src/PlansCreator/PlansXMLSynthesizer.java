package PlansCreator;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.matsim.api.core.v01.Coord;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class PlansXMLSynthesizer {

	private final Random random = new Random();
	private final String outputDirectory;

	// Constructor that accepts the output directory as an argument
	public PlansXMLSynthesizer(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
	public void synthesize(int numPlans) throws ParserConfigurationException, SAXException, IOException {
		List<Coord> householdNodes = extractCoordinates(outputDirectory + "\\households.xml");
		List<Coord> commercialNodes = extractCoordinates(outputDirectory + "\\commercial.xml");

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDirectory + "\\plans.xml"))) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n");
			writer.write("<plans>\n");

			for (int i = 1; i <= numPlans; i++) {
				Coord home = householdNodes.get(random.nextInt(householdNodes.size()));
				Coord work = commercialNodes.get(random.nextInt(commercialNodes.size()));


				writer.write("\t<person id=\"" + i + "\">\n");
				writer.write("\t\t<plan>\n");
				writer.write(String.format("\t\t\t<act type=\"h\" x=\"%.2f\" y=\"%.2f\" end_time=\"%s\" />\n", home.getX(), home.getY(), generateEndTime(7, 30, 15)));
				writer.write("\t\t\t<leg mode=\"car\"> </leg>\n");
				writer.write(String.format("\t\t\t<act type=\"w\" x=\"%.2f\" y=\"%.2f\" start_time=\"%s\" end_time=\"%s\" />\n", work.getX(), work.getY(), generateEndTime(8, 30, 15), generateEndTime(17, 15, 60)));
				writer.write("\t\t\t<leg mode=\"car\"> </leg>\n");
				writer.write(String.format("\t\t\t<act type=\"h\" x=\"%.2f\" y=\"%.2f\" />\n", home.getX(), home.getY()));
				writer.write("\t\t</plan>\n");
				writer.write("\t</person>\n");
				writer.write("\n");
			}

			writer.write("</plans>\n");
		}
	}


	private List<Coord> extractCoordinates(String filePath) throws ParserConfigurationException, SAXException, IOException {
		List<Coord> coordinates = new ArrayList<>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new File(filePath));

		NodeList nodeList = doc.getElementsByTagName("node");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			NamedNodeMap attributes = node.getAttributes();
			double x = Double.parseDouble(attributes.getNamedItem("x").getNodeValue());
			double y = Double.parseDouble(attributes.getNamedItem("y").getNodeValue());
			coordinates.add(new Coord(x, y));
		}

		return coordinates;
	}

	private String generateEndTime(int hour, int minute, int deviationInMinutes) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);

		int deviation = (int) (random.nextGaussian() * deviationInMinutes);
		calendar.add(Calendar.MINUTE, deviation);

		return String.format("%02d:%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
	}

	private String getLegMode(double drtRatio) {
		return random.nextDouble() < drtRatio ? "drt" : "car";
	}

	public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
		String outputDir = "examples/scenarios/UrbanLine/40kmx1km";  // Default directory for this main method
		PlansXMLSynthesizer synthesizer = new PlansXMLSynthesizer(outputDir);
		int numberOfPlansToGenerate = 5000; // specify the desired number of plans here
		synthesizer.synthesize(numberOfPlansToGenerate);
	}

}

