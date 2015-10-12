package playground.benjamin.scenarios.santiago.network;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

public class CreateMergedNetwork {
	
	private static final String svnWorkingDir = "../../shared-svn/"; 	//Path: KT (SVN-checkout)
	private static final String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/inputFromElsewhere/";
	private static final String outputDir = svnWorkingDir + "Kai_und_Daniel/inputForMATSim/network/";		//outputDir of this class -> input for Matsim (KT)

	public static void main(String[] args) {
		createDir(new File(outputDir));

		String crs = "EPSG:32719";
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);
		
		double[] boundingBox1 = new double[]{-71.3607, -33.8875, -70.4169, -33.0144};
		double[] boundingBox2 = new double[]{-70.9, -33.67, -70.47, -33.27};
		double[] boundingBox3 = new double[]{-71.0108, -33.5274, -70.9181, -33.4615};
		
		NetworkImpl network = (NetworkImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		OsmNetworkReader onr = new OsmNetworkReader(network, ct);
		onr.setHierarchyLayer(boundingBox1[3], boundingBox1[0], boundingBox1[1], boundingBox1[2], 4);
		onr.setHierarchyLayer(boundingBox2[3], boundingBox2[0], boundingBox2[1], boundingBox2[2], 5);
		onr.setHierarchyLayer(boundingBox3[3], boundingBox3[0], boundingBox3[1], boundingBox3[2], 5);
		
		onr.parse(workingDirInputFiles+"networkOSM/santiago_tertiary.osm");

		//create connection links (according to e-mail from kt 2015-07-27)
		NetworkFactoryImpl netFactory = (NetworkFactoryImpl) network.getFactory();
		Node node = netFactory.createNode(Id.createNodeId("n_add_01"), new Coord((double) 345165, (double) 6304696));
		network.addNode(node);
		
		Link link01 = netFactory.createLink(Id.createLinkId("l_add_01"), network.getNodes().get(Id.createNodeId("n_add_01")), network.getNodes().get(Id.createNodeId("267315588")), network, 50.2, 40/3.6, 600, 1);
		network.addLink(link01);
		Link link02 = netFactory.createLink(Id.createLinkId("l_add_02"), network.getNodes().get(Id.createNodeId("267315588")), network.getNodes().get(Id.createNodeId("n_add_01")), network, 50.2, 40/3.6, 600, 1);
		network.addLink(link02);
		Link link03 = netFactory.createLink(Id.createLinkId("l_add_03"), network.getNodes().get(Id.createNodeId("267315579")), network.getNodes().get(Id.createNodeId("n_add_01")), network, 58.23, 40/3.6, 600, 1);
		network.addLink(link03);
		Link link04 = netFactory.createLink(Id.createLinkId("l_add_04"), network.getNodes().get(Id.createNodeId("n_add_01")), network.getNodes().get(Id.createNodeId("267315579")), network, 58.23, 40/3.6, 600, 1);
		network.addLink(link04);
		Link link05 = netFactory.createLink(Id.createLinkId("l_add_05"), network.getNodes().get(Id.createNodeId("n_add_01")), network.getNodes().get(Id.createNodeId("267315716")), network, 233.03, 40/3.6, 600, 1);
		network.addLink(link05);
		Link link06 = netFactory.createLink(Id.createLinkId("l_add_06"), network.getNodes().get(Id.createNodeId("267315716")), network.getNodes().get(Id.createNodeId("n_add_01")), network, 233.03, 40/3.6, 600, 1);
		network.addLink(link06);
		
		//remove small streets in the south west of the network
		network.removeLink(Id.createLinkId("4978"));
		network.removeLink(Id.createLinkId("9402"));
		
		//change number of lanes according to kt's e-mail
		int newNLanes = 2;
		network.getLinks().get(Id.createLinkId("10308")).setCapacity(network.getLinks().get(Id.createLinkId("10308")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10308")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10308")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10309")).setCapacity(network.getLinks().get(Id.createLinkId("10309")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10309")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10309")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10310")).setCapacity(network.getLinks().get(Id.createLinkId("10310")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10310")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10310")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10311")).setCapacity(network.getLinks().get(Id.createLinkId("10311")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10311")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10311")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10326")).setCapacity(network.getLinks().get(Id.createLinkId("10326")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10326")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10326")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10327")).setCapacity(network.getLinks().get(Id.createLinkId("10327")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10327")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10327")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10328")).setCapacity(network.getLinks().get(Id.createLinkId("10328")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10328")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10328")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10329")).setCapacity(network.getLinks().get(Id.createLinkId("10329")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10329")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10329")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10330")).setCapacity(network.getLinks().get(Id.createLinkId("10330")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10330")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10330")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10331")).setCapacity(network.getLinks().get(Id.createLinkId("10331")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10331")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10331")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10332")).setCapacity(network.getLinks().get(Id.createLinkId("10332")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10332")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10332")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10333")).setCapacity(network.getLinks().get(Id.createLinkId("10333")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10333")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10333")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10334")).setCapacity(network.getLinks().get(Id.createLinkId("10334")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10334")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10334")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10335")).setCapacity(network.getLinks().get(Id.createLinkId("10335")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10335")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10335")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10336")).setCapacity(network.getLinks().get(Id.createLinkId("10336")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10336")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10336")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("10337")).setCapacity(network.getLinks().get(Id.createLinkId("10337")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("10337")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("10337")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("21383")).setCapacity(network.getLinks().get(Id.createLinkId("21383")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("21383")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("21383")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("21384")).setCapacity(network.getLinks().get(Id.createLinkId("21384")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("21384")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("21384")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("21381")).setCapacity(network.getLinks().get(Id.createLinkId("21381")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("21381")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("21381")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("21382")).setCapacity(network.getLinks().get(Id.createLinkId("21382")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("21382")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("21382")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("16272")).setCapacity(network.getLinks().get(Id.createLinkId("16272")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16272")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16272")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("16273")).setCapacity(network.getLinks().get(Id.createLinkId("16273")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16273")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16273")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("16270")).setCapacity(network.getLinks().get(Id.createLinkId("16270")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16270")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16270")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("16271")).setCapacity(network.getLinks().get(Id.createLinkId("16271")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16271")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16271")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("16268")).setCapacity(network.getLinks().get(Id.createLinkId("16268")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16268")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16268")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("16269")).setCapacity(network.getLinks().get(Id.createLinkId("16269")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("16269")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("16269")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("19484")).setCapacity(network.getLinks().get(Id.createLinkId("19484")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("19484")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("19484")).setNumberOfLanes(newNLanes);
		
		network.getLinks().get(Id.createLinkId("19485")).setCapacity(network.getLinks().get(Id.createLinkId("19485")).getCapacity() * newNLanes / network.getLinks().get(Id.createLinkId("19485")).getNumberOfLanes());
		network.getLinks().get(Id.createLinkId("19485")).setNumberOfLanes(newNLanes);
		
		new NetworkCleaner().run(network);
		
		new NetworkWriter(network).write(outputDir + "network_merged_cl.xml.gz");
		
		convertNet2Shape(network, crs, outputDir+"networkShp/"); 
		calcMinMaxCoord(network);
		
		System.out.println("### Finished network creation. ###");
		
	}
	
	private static void convertNet2Shape(Network network, String crs, String outputDir){
		
		createDir(new File(outputDir));
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("link").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("type", String.class).
				addAttribute("capacity", Double.class).
				addAttribute("freespeed", Double.class).
				create();

		for (Link link : network.getLinks().values()) {
			Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), link.getLength(), ((LinkImpl)link).getType(), link.getCapacity(), link.getFreespeed()}, null);
			features.add(ft);
		}   
		ShapeFileWriter.writeGeometries(features, outputDir + "network_merged_cl_Links.shp");

		features.clear();

		PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("nodes").
				addAttribute("ID", String.class).
				create();

		for (Node node : network.getNodes().values()) {
			SimpleFeature ft = nodeFactory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, null);
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, outputDir + "network_merged_cl_Nodes.shp");
	}
	
	private static void calcMinMaxCoord(Network network) {
		double[] box = NetworkUtils.getBoundingBox(network.getNodes().values());
		System.out.println("Network bounding box:");
		System.out.println("minX "+ box[0]);
		System.out.println("minY "+ box[1]);
		System.out.println("maxX "+ box[2]);
		System.out.println("maxY "+ box[3]);
	}
	
	private static void createDir(File file) {
		System.out.println("Directory " + file + " created: "+ file.mkdirs());	
	}

}
