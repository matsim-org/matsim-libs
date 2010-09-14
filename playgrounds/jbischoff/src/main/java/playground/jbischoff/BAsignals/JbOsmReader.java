package playground.jbischoff.BAsignals;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.run.NetworkCleaner;
import org.xml.sax.SAXException;


public class JbOsmReader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		NetworkLayer network = new NetworkLayer();
		OsmNetworkReader osmReader = new OsmNetworkReader(network,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.WGS84_UTM33N), false);
		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);
		String input = "/Users/JB/Desktop/BA-Arbeit/sim/brandenburg.osm";
		String output = "/Users/JB/Desktop/BA-Arbeit/sim/brandenburg";
		
//		 set osmReader useHighwayDefaults false 
//		 Autobahn
		osmReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		osmReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
//		 Bundesstrasse?
		osmReader.setHighwayDefaults(1, "trunk",         1,  80.0/3.6, 1.0, 2000);
		osmReader.setHighwayDefaults(1, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
//		 Durchgangsstrassen
		osmReader.setHighwayDefaults(1, "primary",       1,  80.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(1, "primary_link",  1,  60.0/3.6, 1.0, 1500);
//		 Hauptstrassen
		osmReader.setHighwayDefaults(1, "secondary",     1,  60.0/3.6, 1.0, 1000);
//		 mehr Hauptstrassen
		osmReader.setHighwayDefaults(1, "tertiary",      1,  45.0/3.6, 1.0,  600);
//		 Nebenstrassen
		osmReader.setHighwayDefaults(2, "minor",         1,  45.0/3.6, 1.0,  600);
//		 diverse
		osmReader.setHighwayDefaults(2, "unclassified",  1,  45.0/3.6, 1.0,  600);
//		 Wohngebiete
		osmReader.setHighwayDefaults(2, "residential",   1,  30.0/3.6, 1.0,  600);
//		 Spielstrassen irrelevant, since only tiny percentile
//		 osmReader.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);
		osmReader.setHierarchyLayer(52.04382,13.499222, 51.258248,14.887619, 1);
		osmReader.setHierarchyLayer(51.820578,14.247866, 51.684789,14.507332, 2);
		
		try {
			osmReader.parse(input);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Write network to file
		new NetworkWriter(network).write(output + ".xml.gz");
		System.out.println("Done! Unprocessed MATSim Network saved as " + output + ".xml.gz");
		// Clean network
		new NetworkCleaner().run(new String[] {output + ".xml.gz", output + "_cl.xml.gz"});
		System.out.println("NetworkCleaner done! Network saved as " + output + "_cl.xml.gz");
		

	

	}

}
