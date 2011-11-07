package playground.sergioo.NetworksMatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.sergioo.NetworksMatcher.gui.DoubleNetworkCapacitiesWindow;
import playground.sergioo.NetworksMatcher.kernel.CrossingMatchingStep;
import playground.sergioo.Visualizer2D.LayersWindow;

import com.vividsolutions.jts.geom.Coordinate;

public class ApplyCapacities {

	//Constants

	public static final File MATCHINGS_FILE = new File("./data/matching/matchings.txt");
	
	//Attributes

	//Methods
	
	public static Network getNetworkFromShapeFileLength(String fileName) {
		ShapeFileReader shapeFileReader =  new ShapeFileReader();
		Set<Feature> features = shapeFileReader.readFileAndInitialize(fileName);
		Network network = NetworkImpl.createNetwork();
		NetworkFactory networkFactory = network.getFactory();
		for(Feature feature:features)
			if(feature.getFeatureType().getTypeName().equals("emme_links")) {
				Coordinate[] coords = feature.getDefaultGeometry().getCoordinates();
				Id idFromNode = new IdImpl((Long)feature.getAttribute("INODE"));
				Node fromNode = network.getNodes().get(idFromNode);
				if(fromNode==null) {
					fromNode = networkFactory.createNode(idFromNode, new CoordImpl(coords[0].x, coords[0].y));
					network.addNode(fromNode);
				}
				Id idToNode = new IdImpl((Long)feature.getAttribute("JNODE"));
				Node toNode = network.getNodes().get(idToNode);
				if(toNode==null) {
					toNode = networkFactory.createNode(idToNode, new CoordImpl(coords[coords.length-1].x, coords[coords.length-1].y));
					network.addNode(toNode);
				}
				Link link = network.getFactory().createLink(new IdImpl(idFromNode+"-->"+idToNode), fromNode, toNode);
				link.setCapacity((Double)feature.getAttribute("DATA2"));
				link.setNumberOfLanes((Double)feature.getAttribute("LANES"));
				link.setLength((Double)feature.getAttribute("LENGTH"));
				((LinkImpl)link).setOrigId(feature.getID());
				network.addLink(link);
			}
		return network;
	}
	
	public static Map<Link, Tuple<Link,Double>> loadCapacities(File file, Network networkA, Network networkB) {
		Map<Link, Tuple<Link,Double>> linksChanged = new HashMap<Link, Tuple<Link,Double>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(":::");
				linksChanged.put(networkB.getLinks().get(new IdImpl(parts[0])), new Tuple<Link,Double>(networkA.getLinks().get(new IdImpl(parts[1])), Double.parseDouble(parts[2])));
				line = reader.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return linksChanged;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
		matsimNetworkReader.readFile(args[0]);
		Network networkLowResolution = getNetworkFromShapeFileLength(args[1]);
		Network networkHighResolution = scenario.getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
		for(Node node:networkLowResolution.getNodes().values())
			((NodeImpl)node).setCoord(coordinateTransformation.transform(node.getCoord()));
		CrossingMatchingStep.CAPACITIES_FILE = new File("./data/matching/capacities/linksChanged.txt");
		Map<Link, Tuple<Link,Double>> result = loadCapacities(CrossingMatchingStep.CAPACITIES_FILE, networkLowResolution, networkHighResolution);
		LayersWindow windowHR2 = new DoubleNetworkCapacitiesWindow("Networks capacities", networkLowResolution, networkHighResolution, result);
		windowHR2.setVisible(true);
		while(!windowHR2.isReadyToExit())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
}
