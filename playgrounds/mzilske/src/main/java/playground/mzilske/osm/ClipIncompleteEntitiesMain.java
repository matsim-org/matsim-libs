package playground.mzilske.osm;

import java.io.File;

import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;
import org.openstreetmap.osmosis.core.xml.v0_6.XmlWriter;

public class ClipIncompleteEntitiesMain {
	
	public static void main(String[] args) {
		FastXmlReader reader = new FastXmlReader(new File("/Users/michaelzilske/osm/motorway_germany.osm"), true, CompressionMethod.None);
		ClipIncompleteEntities clipper = new ClipIncompleteEntities(IdTrackerType.BitSet, true, true, true);
		XmlWriter writer = new XmlWriter(new File("/Users/michaelzilske/osm/clipped.osm"), CompressionMethod.None);
		reader.setSink(clipper);
		clipper.setSink(writer);
		reader.run();
	}

}
