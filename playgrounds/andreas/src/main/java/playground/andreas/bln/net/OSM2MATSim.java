package playground.andreas.bln.net;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkSegmentDoubleLinks;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.run.NetworkCleaner;
import org.xml.sax.SAXException;


public class OSM2MATSim {

	// TODO [an] keep attributes like cycleway and pedestrian in mind, but block access to motorways for those users
	
	public static void main(final String[] args) {

		NetworkImpl network = NetworkImpl.createNetwork();
//		OsmNetworkReader osmReader = new OsmNetworkReader(network, new WGS84toCH1903LV03());
		OsmNetworkReader osmReader = new OsmNetworkReader(network,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.DHDN_GK4), false);
		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);
		
//		String inputFile = "./berlin.osm.gz";
//		String outputFile = "./test";
		
		String inputFile = args[0];
		String outputFile = args[1];
		
		// Anmerkung trunk, primary und secondary sollten in Bln als ein Typ behandelt werden
		
		// Autobahn
		osmReader.setHighwayDefaults(1, "motorway",      2,  100.0/3.6, 1.2, 2000, true); // 70
		osmReader.setHighwayDefaults(1, "motorway_link", 1,  60.0/3.6, 1.2, 1500, true); // 60
		// Pseudoautobahn
		osmReader.setHighwayDefaults(2, "trunk",         2,  50.0/3.6, 0.5, 1000); // 45
		osmReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 0.5, 1000); // 40
		// Durchgangsstrassen
		osmReader.setHighwayDefaults(3, "primary",       1,  50.0/3.6, 0.5, 1000); // 35
		osmReader.setHighwayDefaults(3, "primary_link",  1,  50.0/3.6, 0.5, 1000); // 30
		
		// Hauptstrassen
		osmReader.setHighwayDefaults(4, "secondary",     1,  50.0/3.6, 0.5, 1000); // 30
		// Weitere Hauptstrassen
		osmReader.setHighwayDefaults(5, "tertiary",      1,  30.0/3.6, 0.8,  600); // 25 
		// bis hier ca wip
		
		// Nebenstrassen
		osmReader.setHighwayDefaults(6, "minor",         1,  30.0/3.6, 0.8,  600); // nix
		// Alles Moegliche, vor allem Nebenstrassen auf dem Land, meist keine 30er Zone 
		osmReader.setHighwayDefaults(6, "unclassified",  1,  30.0/3.6, 0.8,  600);
		// Nebenstrassen, meist 30er Zone
		osmReader.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 0.6,  600);
		// Spielstrassen
		osmReader.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);
		
		// Fahrrad
		osmReader.setHighwayDefaults(7, "cycleway", 1,  14.0/3.6, 1.0,  300);
		// Fussgaenger
		osmReader.setHighwayDefaults(8, "pedestrian", 1,  3.0/3.6, 1.0,  300);
		osmReader.setHighwayDefaults(8, "footway", 1,  3.0/3.6, 1.0,  300);
		osmReader.setHighwayDefaults(8, "service", 1,  3.0/3.6, 1.0,  300);
		osmReader.setHighwayDefaults(8, "steps", 1,  3.0/3.6, 1.0,  300);
		
//		osmReader.setHierarchyLayer(55.5, 4.5, 47.5, 15.5, 1); // Deutschland
//		osmReader.setHierarchyLayer(54.3, 10.0, 50.8, 15.0, 2); // Brandenburg mit Autobahnstummeln
//		osmReader.setHierarchyLayer(53.6, 11.2, 51.2, 15.0, 4); // BerlinBrandenburg
		
//		osmReader.setHierarchyLayer(53.6, 11.5, 52.8, 14.5, 4); // BerlinBrandenburg - Streifen 1
//		osmReader.setHierarchyLayer(53.0, 12.1, 52.0, 14.9, 4); // BerlinBrandenburg - Quadrat
//		osmReader.setHierarchyLayer(52.2, 13.0, 51.4, 14.9, 4); // BerlinBrandenburg - Streifen 2
		
//		osmReader.setHierarchyLayer(52.62, 13.2, 52.37, 13.7, 5); // Stadtbereich Bln
//		osmReader.setHierarchyLayer(52.43, 12.97, 52.35, 13.16, 5); // Potsdam
//		osmReader.setHierarchyLayer(52.56, 13.13, 52.51, 13.21, 5); // Spandau

//		osmReader.setHierarchyLayer(52.494, 13.413, 52.461, 13.440, 6); // Hermannstrasse
		osmReader.setHierarchyLayer(52.51, 13.37, 52.38, 13.54, 5); // M44/344 weitere Umgebung
		osmReader.setHierarchyLayer(52.495, 13.410, 52.407, 13.465, 6); // M44/344 komplett		
		
		
		
//		osmReader.setHierarchyLayer(52.642299, 13.304882, 52.527397, 13.805398, 5);
//		osmReader.setHierarchyLayer(52.537028, 13.410000, 52.520000, 13.443527, 8);
		
//		osmReader.setHierarchyLayer(52.742845, 12.905454, 52.206321, 13.414334, 2);
//		osmReader.setHierarchyLayer(52.408424, 13.001725, 52.393787, 13.070721, 8);
//		osmReader.setHierarchyLayer(52.410267, 13.028828, 52.379898, 13.086545, 5);
//		
//		osmReader.setHierarchyLayer(52.642299, 13.304882, 52.527397, 13.805398, 5);
//		osmReader.setHierarchyLayer(52.537028, 13.410000, 52.520000, 13.443527, 8);
		
		// POA Berlin Hundekopf
//		osmReader.setHierarchyLayer(52.565, 13.265, 52.45, 13.5, 1);
//		osmReader.setHierarchyLayer(52.56, 13.27, 52.458, 13.485, 4);
		// POA Berlin Korridor
//		osmReader.setHierarchyLayer(52.52, 13.145, 52.504, 13.38, 4);
		// POA Berlin Korridor Ana
//		osmReader.setHierarchyLayer(52.491, 13.315, 52.53, 13.433, 8);
		
		try {
			osmReader.parse(inputFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("...done");
		
		new NetworkWriter(network).write(outputFile + ".xml.gz");
		new NetworkCleaner().run(new String[] {outputFile + ".xml.gz", outputFile + "_cl.xml.gz"});
		System.out.println("NetworkCleaner...done");

		// Simplifier
		Scenario scenario = new ScenarioImpl();
		network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(outputFile + "_cl.xml.gz");

		NetworkSimplifier nsimply = new NetworkSimplifier();
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		nsimply.setNodesToMerge(nodeTypesToMerge);
//		nsimply.setMergeLinkStats(true);
		nsimply.run(network);
		new NetworkWriter(network).write(outputFile + "_cl_simple.xml.gz");
		System.out.println("NetworkSimplifier...done");
		
		NetworkSegmentDoubleLinks networkDoubleLinks = new NetworkSegmentDoubleLinks();
		networkDoubleLinks.run(network);
		new NetworkWriter(network).write(outputFile + "_cl_simple_dl.xml.gz");
		System.out.println("NetworkDoubleLinks...done");
		System.out.println("Converting...done");

	}

}
