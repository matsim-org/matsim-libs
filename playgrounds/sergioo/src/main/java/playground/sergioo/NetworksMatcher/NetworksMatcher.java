package playground.sergioo.NetworksMatcher;

import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.sergioo.NetworkVisualizer.gui.NetworkWindow;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.SimpleNetworkPainter;
import playground.sergioo.NetworksMatcher.gui.DoubleNetworkWindow;
import playground.sergioo.NetworksMatcher.kernel.MatchingProcess;

import com.vividsolutions.jts.geom.Coordinate;

public class NetworksMatcher {


	//Main

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
		matsimNetworkReader.readFile(args[0]);
		/*Network networkLowResolutionPolyline = getNetworkFromShapeFilePolyline(args[1]);
		NetworkWindow windowLRP = new SimpleNetworkWindow("Low Resolution Network Polyline", new SimpleNetworkPainter(networkLowResolutionPolyline));
		windowLRP.setVisible(true);*/
		Network networkHighResolution = scenario.getNetwork();
		Network networkLowResolutionLength = getNetworkFromShapeFileLength(args[1]);
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
		for(Node node:networkLowResolutionLength.getNodes().values())
			((NodeImpl)node).setCoord(coordinateTransformation.transform(node.getCoord()));
		NetworkWindow windowHR = new DoubleNetworkWindow("High Resolution Network", new SimpleNetworkPainter(networkHighResolution), new SimpleNetworkPainter(networkLowResolutionLength));
		windowHR.setVisible(true);
		while(!windowHR.isReadyToExit())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		((DoubleNetworkWindow)windowHR).setNetworksSeparated();
		windowHR.setVisible(true);
		MatchingProcess matchingProcess = new MatchingProcess();
		matchingProcess.execute(networkHighResolution, networkLowResolutionLength);
		//matchingProcess.applyProperties(false);
		System.out.println(networkHighResolution.getLinks().size()+" "+networkLowResolutionLength.getLinks().size());
	}


	//Methods

	public static Network getNetworkFromShapeFilePolyline(String fileName) {
		ShapeFileReader shapeFileReader =  new ShapeFileReader();
		Set<Feature> features = shapeFileReader.readFileAndInitialize(fileName);
		Network network = NetworkImpl.createNetwork();
		NetworkFactory networkFactory = network.getFactory();
		long nodeLongId=0, linkLongId=0;
		for(Feature feature:features)
			if(feature.getFeatureType().getTypeName().equals("emme_links")) {
				Coordinate[] coords = feature.getDefaultGeometry().getCoordinates();
				Node[] nodes = new Node[coords.length];
				for(int n=0; n<nodes.length; n++) {
					Coord coord = new CoordImpl(coords[n].x, coords[n].y);
					for(Node node:network.getNodes().values())
						if(node.getCoord().equals(coord))
							nodes[n] = node;
					if(nodes[n]==null) {
						nodes[n] = networkFactory.createNode(new IdImpl(nodeLongId), coord);
						nodeLongId++;
						if(n==0)
							((NodeImpl)nodes[n]).setOrigId(feature.getAttribute("INODE").toString());
						else if(n==nodes.length-1)
							((NodeImpl)nodes[n]).setOrigId(feature.getAttribute("JNODE").toString());
					}
				}
				for(int n=0; n<nodes.length-1; n++) {
					if(network.getNodes().get(nodes[n].getId())==null)
						network.addNode(nodes[n]);
					Link link = network.getFactory().createLink(new IdImpl(linkLongId), nodes[n], nodes[n+1]);
					link.setCapacity((Double)feature.getAttribute("DATA2"));
					link.setNumberOfLanes((Double)feature.getAttribute("LANES"));
					((LinkImpl)link).setOrigId(feature.getID());
					network.addLink(link);
					linkLongId++;
				}
				if(network.getNodes().get(nodes[nodes.length-1].getId())==null)
					network.addNode(nodes[nodes.length-1]);
			}
		return network;
	}
	
	public static Network getNetworkFromShapeFileLength(String fileName) {
		ShapeFileReader shapeFileReader =  new ShapeFileReader();
		Set<Feature> features = shapeFileReader.readFileAndInitialize(fileName);
		Network network = NetworkImpl.createNetwork();
		NetworkFactory networkFactory = network.getFactory();
		long linkLongId=0;
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
				Link link = network.getFactory().createLink(new IdImpl(linkLongId), fromNode, toNode);
				link.setCapacity((Double)feature.getAttribute("DATA2"));
				link.setNumberOfLanes((Double)feature.getAttribute("LANES"));
				link.setLength((Double)feature.getAttribute("LENGTH"));
				((LinkImpl)link).setOrigId(feature.getID());
				network.addLink(link);
				linkLongId++;
			}
		return network;
	}


}
