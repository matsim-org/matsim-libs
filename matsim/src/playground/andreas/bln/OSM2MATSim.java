package playground.andreas.bln;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.run.NetworkCleaner;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.utils.io.OsmNetworkReader;
import org.xml.sax.SAXException;

public class OSM2MATSim {

	public static void main(final String[] args) {

		NetworkLayer network = new NetworkLayer();
//		OsmNetworkReader osmReader = new OsmNetworkReader(network, new WGS84toCH1903LV03());
		OsmNetworkReader osmReader = new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4), -1);
		osmReader.setKeepPaths(false);
		
		// Autobahn
		osmReader.setHighwayDefaults("motorway",      2, 120.0/3.6, 2000, true);
		osmReader.setHighwayDefaults("motorway_link", 1,  80.0/3.6, 1500, true);
		// Pseudoautobahn
		osmReader.setHighwayDefaults("trunk",         2,  80.0/3.6, 2000);
		osmReader.setHighwayDefaults("trunk_link",    1,  50.0/3.6, 1500);
		// Durchgangsstrassen
		osmReader.setHighwayDefaults("primary",       1,  60.0/3.6, 1500);
		osmReader.setHighwayDefaults("primary_link",  1,  50.0/3.6, 1500);
		
		// Hauptstrassen
		osmReader.setHighwayDefaults("secondary",     1,  50.0/3.6, 1000);
		// Weitere Hauptstrassen
		osmReader.setHighwayDefaults("tertiary",      1,  45.0/3.6,  600); // ca wip
		
		// Nebenstrassen
		osmReader.setHighwayDefaults("minor",         1,  45.0/3.6,  600); // nix
		// Alles Mögliche, vor allem Nebenstrassen auf dem Land, meist keine 30er Zone 
		osmReader.setHighwayDefaults("unclassified",  1,  45.0/3.6,  600);
		// Nebenstrassen, meist 30er Zone
		osmReader.setHighwayDefaults("residential",   1,  30.0/3.6,  600);
		// Spielstrassen
		osmReader.setHighwayDefaults("living_street", 1,  15.0/3.6,  300);
		
		
		try {
			osmReader.parse("z:/osm_net/20090316_berlinbrandenburg.fused.gz");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new NetworkWriter(network, "z:/osm_net/test.xml").write();
		new NetworkCleaner().run(new String[] {"z:/osm_net/test.xml", "z:/osm_net/test_cl.xml"});

	}

}
