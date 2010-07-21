package playground.mzilske.osm;

import java.io.File;

import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;

public class OsmTransitMain {
	
	public static void main(String[] args) {
		String filename = "inputs/schweiz/zurich.osm";
		FastXmlReader reader = new FastXmlReader(new File(filename ), true, CompressionMethod.None);		
		TransitNetworkSink transitNetworkSink = new TransitNetworkSink();
		reader.setSink(transitNetworkSink);
		reader.run();
	}

}
