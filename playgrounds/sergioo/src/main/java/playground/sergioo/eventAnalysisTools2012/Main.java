package playground.sergioo.eventAnalysisTools2012;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vehicles.Vehicle;

public class Main {

	//Constants
	private final static String[] NODES = {"fl1380023697", "fl1380023696", "1380018339", "1380018369", "1380005056", "fl1380005050", "1380005051", "fl1380005104", "fl1380005105", "fl1380017582", "1380007412", "fl1380007413", "fl1380007414", "fl1380007415", "1380007417", "1380004301", "fl1380026056", "1380003771", "fl1380028836", "fl1380028837", "1380037616", "fl1380042204", "fl1380034444", "1380026126", "1380008911", "fl1380009667", "fl1380009665", "fl1380009666", "fl1380009668", "fl1380009669", "1380032058", "fl1380015859", "fl1380015843", "fl1380015844", "fl1380006426", "fl1380006427", "fl1380040290", "1380018558", "fl1380018556", "1380018557", "1380020283", "1380003526", "1380003527", "1380003528", "1380009663", "1380009664", "1380033576", "1380026356", "1380017055", "1380017054", "1380012775", "1380012773", "1380012774", "1380017592", "1380017593", "1380000030", "1380000031", "1380000032", "1380017358", "1380017359", "1380000027", "cl1380000028", "1380000029", "cl70234_1380029418_0", "1380029418", "1380031999", "1380009482", "1380009483", "1380009484", "1380029570", "1380015934", "1380030786", "1380030787", "1380028314", "cl16569_1380027040_0", "1380027040", "1380033272", "1380027345", "1380000233", "cl1380000231", "1380000232", "1380004109", "1380019523", "1380018689", "1380018690", "1380032270", "1380019741", "1380019739", "1380019740", "1380022838", "1380005392", "1380032271", "1380007256", "1380019743", "1380026078", "cl1380019534", "cl57073_1380019532_0", "cl1380019532", "1380019533", "cl3016_1380035914_0", "1380035914", "1380038486", "cl29981_1380035485_0", "1380035485", "1380031465", "1380026568", "cl30724_1380009568_0", "cl1380009568", "cl1380009569", "1380005328", "cl1380005329", "1380005330", "1380011984", "1380011975", "1380011976", "1380011981", "1380011982", "1380011991", "1380000760", "56658_1380013293_0", "1380013293", "1380013289", "1380003312", "1380038427", "1380032904", "1380017577", "1380036297", "1380028071", "1380029055", "1380017558", "1380017559", "1380020535", "1380020536", "1380026467", "1380026057", "1380018491", "1380018492", "1380009744", "1380009740", "1380002083", "1380025539", "1380033610", "1380019378", "56042_1380019176_0", "1380019176", "1380042135", "1380025865", "1380037697", "1380024474", "1380024475", "1380017969", "1380001256", "1380029038", "1380032546", "1380024999", "1380038103", "1380005364", "1380024910", "1380024911", "1380028605", "1380028598", "1380019026", "1380028398", "1380025211", "1380025210"};

	//Main
	/**
	 * @param args
	 * @throws IOException 
	 */
	/*public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore6.xml");
		double dist = 0;
		for(int i=0; i<NODES.length-1; i++)
			for(Link link:scenario.getNetwork().getLinks().values())
				if(scenario.getNetwork().getNodes().get(Id.createNodeId(NODES[i])).equals(link.getFromNode()) && scenario.getNetwork().getNodes().get(Id.createNodeId(NODES[i+1])).equals(link.getToNode()))
					dist+=link.getLength();
		System.out.println(dist);
	}*/
	/*public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore5.xml");
		for(Link link:scenario.getNetwork().getLinks().values())
			if(link.getAllowedModes().contains("bus") && link.getAllowedModes().contains("car"))
				link.setCapacity(link.getCapacity()+(link.getCapacity()/link.getNumberOfLanes())*3);
		new NetworkWriter(scenario.getNetwork()).write("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
	}*/
	public static void main(String[] args) throws IOException {
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
		new NetworkCleaner().run(scenario.getNetwork());
		TravelDisutility travelMinCost =  new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength()/link.getFreespeed();
			}
		};
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(scenario.getNetwork());
		TravelTime timeFunction = new TravelTime() {	

			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength()/link.getFreespeed();
			}
		};
		LeastCostPathCalculatorFactory routerFactory = new FastDijkstraFactory(preProcessData);
		LeastCostPathCalculator leastCostPathCalculator = routerFactory.createPathCalculator(scenario.getNetwork(), travelMinCost, timeFunction);
		BufferedReader reader = new BufferedReader(new FileReader(new File("./data/MTZ_Shortest_Path_Yanding.txt")));
		PrintWriter writer = new PrintWriter(new File("./data/MTZ_Shortest_Path_Yanding_Full.txt"));
		String line=reader.readLine();
		int i=0;
		while(line!=null) {
			String[] parts=line.split(";");
			Coord start = coordinateTransformation.transform(new Coord(Double.parseDouble(parts[3]), Double.parseDouble(parts[4])));
			Coord end = coordinateTransformation.transform(new Coord(Double.parseDouble(parts[5]), Double.parseDouble(parts[6])));
			Path path = leastCostPathCalculator.calcLeastCostPath(((NetworkImpl)scenario.getNetwork()).getNearestNode(start), ((NetworkImpl)scenario.getNetwork()).getNearestNode(end), 0, null, null);
			double distance = 0;
			for(Link link:path.links)
				distance+=link.getLength();
			writer.println(parts[0]+";"+path.travelCost+";"+distance);
			line=reader.readLine();
			i++;
			if(i%1000==0)
				System.out.println(i);
		}
		reader.close();
		writer.close();
	}
}
