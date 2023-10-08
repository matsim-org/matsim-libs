package TransitCreator;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Collections;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author  Yo Kamijo
 * This is an example script to create taxi vehicle files.
 *
 */

public class RunCreateDrtMesh {

	public static void addVehicles(List<DvrpVehicleSpecification> vehicles, int count, Link link, int size, int seats1, double operationStartTime, double operationEndTime) {
        for (int i = 0; i < size; i++) {
            vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                    .id(Id.create("drt" + count, DvrpVehicle.class))
                    .startLinkId(link.getId())
                    .capacity(seats1)
                    .serviceBeginTime(operationStartTime)
                    .serviceEndTime(operationEndTime)
                    .build());
            count += 1;
        }
    }
	public static void main(String[] args)  {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		double operationStartTime = 0.;
		double operationEndTime = 32*3600.;
		int seats = 4;
		int size = 1;
		int count = 0;
		String xmlFilePath = "examples/scenarios/UrbanLine/3x1km/network.xml";  // adjust path accordingly
		String outputDirectory = "examples/scenarios/UrbanLine/3x1km/output";
		String drtsFile = outputDirectory + "drts" + count + "S" + seats + ".xml";
		List<List<String>> records = new ArrayList<List<String>>();
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFilePath);

			NodeList nodeList = doc.getElementsByTagName("node");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element nodeElement = (Element) nodeList.item(i);
				String id = nodeElement.getAttribute("id");
				String x = nodeElement.getAttribute("x");
				String y = nodeElement.getAttribute("y");
				records.add(Arrays.asList(id, x, y));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		//ヘッダーを消すコマンド（ヘッダーも読み込まれていくため）
		records.remove(0);

		// Shuffle the records randomly and get only the first 10% of the nodes
		Collections.shuffle(records, new Random());
		int tenPercentSize = (int) (0.025 * records.size());
		records = records.subList(0, tenPercentSize);

		System.out.println(records);

		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(xmlFilePath);
		Network network = scenario.getNetwork();
		for(List<String> row: records) {
			Coord coord = new Coord(Double.parseDouble(row.get(1)),Double.parseDouble(row.get(2)));
			Link startLink = NetworkUtils.getNearestLink(network, coord);
			addVehicles(vehicles, count, startLink, size, seats, operationStartTime, operationEndTime);
			count += size;
		}

		System.out.println(drtsFile);
		new FleetWriter(vehicles.stream()).write(drtsFile);
	}

}
