package playground.mzilske.osm;

import java.io.File;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;

public class OsmMain {
	
	public static void main(String[] args) {
		String filename = "inputs/schweiz/zurich.osm";
		FastXmlReader reader = new FastXmlReader(new File(filename ), true, CompressionMethod.None);
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		MyDatasetSink sink = new MyDatasetSink(coordinateTransformation);
		SimplifyTask simplify = new SimplifyTask(IdTrackerType.BitSet);
		
		reader.setSink(simplify);
		simplify.setSink(sink);
		
		reader.run();
	}

}
