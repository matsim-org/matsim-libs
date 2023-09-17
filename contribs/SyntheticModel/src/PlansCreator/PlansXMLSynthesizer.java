package PlansCreator;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class PlansXMLSynthesizer {

	private final Random random = new Random();
	private static final String DIRECTORY_PATH = "C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine"; // Replace with your directory path


	public void synthesize(int numPlans) throws ParserConfigurationException, SAXException, IOException {
		List<Coordinate> householdNodes = extractCoordinates(DIRECTORY_PATH + "\\households.xml");
		List<Coordinate> commercialNodes = extractCoordinates(DIRECTORY_PATH + "\\commercial.xml");

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(DIRECTORY_PATH + "\\plans.xml"))) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n");
			writer.write("<plans>\n");

			for (int i = 1; i <= numPlans; i++) {
				Coordinate home = householdNodes.get(random.nextInt(householdNodes.size()));
				Coordinate work = commercialNodes.get(random.nextInt(commercialNodes.size()));

				writer.write("\t<person id=\"" + i + "\">\n");
				writer.write("\t\t<plan>\n");
				writer.write(String.format("\t\t\t<act type=\"h\" x=\"%.2f\" y=\"%.2f\" end_time=\"%s\" />\n", home.x, home.y, generateEndTime(7, 30, 15)));
				writer.write("\t\t\t<leg mode=\"car\"> </leg>\n");
				writer.write(String.format("\t\t\t<act type=\"w\" x=\"%.2f\" y=\"%.2f\" end_time=\"%s\" />\n", work.x, work.y, generateEndTime(17, 15, 60)));
				writer.write("\t\t\t<leg mode=\"car\"> </leg>\n");
				writer.write(String.format("\t\t\t<act type=\"h\" x=\"%.2f\" y=\"%.2f\" />\n", home.x, home.y));
				writer.write("\t\t</plan>\n");
				writer.write("\t</person>\n");
				writer.write("\n");
			}

			writer.write("</plans>\n");
		}
	}


	private List<Coordinate> extractCoordinates(String filePath) throws ParserConfigurationException, SAXException, IOException {
		List<Coordinate> coordinates = new ArrayList<>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new File(filePath));

		NodeList nodeList = doc.getElementsByTagName("node");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			NamedNodeMap attributes = node.getAttributes();
			double x = Double.parseDouble(attributes.getNamedItem("x").getNodeValue());
			double y = Double.parseDouble(attributes.getNamedItem("y").getNodeValue());
			coordinates.add(new Coordinate(x, y));
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

	public static void main(String[] args) {
		PlansXMLSynthesizer synthesizer = new PlansXMLSynthesizer();
		int numberOfPlansToGenerate = 10000; // specify the desired number of plans here
		try {
			synthesizer.synthesize(numberOfPlansToGenerate);
		} catch (Exception e) {
			e.printStackTrace();
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
}

