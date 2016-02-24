package others.sergioo.androidAppExtras2014;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


public class NetworkConverter {

	private static final double MIN_DISTANCE = 5.0;

	public static void main0(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM48N, TransformationFactory.WGS84);
		writeNetwork(scenario.getNetwork(), args[1], transformation);
	}
	
	private static void writeNetwork(Network network, String fileName, CoordinateTransformation transformation) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(fileName);
		Set<Node> nodes = new HashSet<Node>();
		Set<Link> links = new HashSet<Link>();
		for(Link link:network.getLinks().values()) {
			nodes.add(link.getFromNode());
			nodes.add(link.getToNode());
			links.add(link);
		}
		writer.println("Nodes");
		for(Node node:nodes) {
			Coord coord = transformation.transform(node.getCoord());
			writer.println(node.getId()+" "+coord.getY()+" "+coord.getX());
		}
		writer.println("Links");
		for(Link link:links)
			writer.println(link.getFromNode().getId()+" "+link.getToNode().getId()+" 1");
		writer.close();
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		Network network = getNetworkFromShapeFilePolyline(args[0]);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84);
		writeNetwork(network, args[1], transformation);
	}
	
	public static Network getNetworkFromShapeFilePolyline(String fileName) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
		Network network = NetworkImpl.createNetwork();
		NetworkFactory networkFactory = network.getFactory();
		long nodeLongId=0, linkLongId=0;
		for(SimpleFeature feature:features) {
			Coordinate[] coords = ((Geometry) feature.getDefaultGeometry()).getCoordinates();
			Node[] nodes = new Node[coords.length];
			for(int n=0; n<nodes.length; n++) {
				Coord coord = new Coord(coords[n].x, coords[n].y);
				for(Node node:network.getNodes().values())
					if(node.getCoord().equals(coord))
						nodes[n] = node;
				if(nodes[n]==null) {
					nodes[n] = networkFactory.createNode(Id.createNodeId(nodeLongId), coord);
					nodeLongId++;
					((NodeImpl)nodes[n]).setOrigId(feature.getAttribute("OBJECTID").toString());
				}
			}
			Node prevNode = nodes[0];
			for(int n=0; n<nodes.length-1; n++) {
				if(network.getNodes().get(nodes[n].getId())==null) {
					if(n+1==nodes.length-1 || CoordUtils.calcEuclideanDistance(prevNode.getCoord(), nodes[n+1].getCoord())>MIN_DISTANCE) {
						network.addNode(nodes[n]);
						Link link = network.getFactory().createLink(Id.createLinkId(linkLongId), prevNode, nodes[n+1]);
						((LinkImpl)link).setOrigId(feature.getID());
						network.addLink(link);
						linkLongId++;
						link = network.getFactory().createLink(Id.createLinkId(linkLongId), nodes[n+1], prevNode);
						((LinkImpl)link).setOrigId(feature.getID());
						network.addLink(link);
						linkLongId++;
						prevNode = nodes[n+1];
					}
				}
				else {
					Link link = network.getFactory().createLink(Id.createLinkId(linkLongId), prevNode, nodes[n+1]);
					((LinkImpl)link).setOrigId(feature.getID());
					network.addLink(link);
					linkLongId++;
					link = network.getFactory().createLink(Id.createLinkId(linkLongId), nodes[n+1], prevNode);
					((LinkImpl)link).setOrigId(feature.getID());
					network.addLink(link);
					linkLongId++;
					prevNode = nodes[n+1];
				}
			}
			if(network.getNodes().get(nodes[nodes.length-1].getId())==null)
				network.addNode(nodes[nodes.length-1]);
		}
		return network;
	}

}

