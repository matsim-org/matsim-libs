package playground.mzilske.osm;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;

public class OsmMain {
	
	public static void main(String[] args) {
		Map<String, Set<String>> filterTags = new HashMap<String, Set<String>>();
		filterTags.put("highway", new HashSet<String>(Arrays.asList("motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street")));
		String filename = "inputs/schweiz/zurich.osm";
		Set<String> emptySet = Collections.emptySet();
		TagFilter tagFilter = new TagFilter(
				"accept-way",
				emptySet,
				filterTags);
		FastXmlReader reader = new FastXmlReader(new File(filename ), true, CompressionMethod.None);
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);
		NetworkWriterSink sink = new NetworkWriterSink(coordinateTransformation);
		SimplifyTask simplify = new SimplifyTask(IdTrackerType.BitSet);
		
		reader.setSink(tagFilter);
		tagFilter.setSink(simplify);
		
//		reader.setSink(simplify);
		
		simplify.setSink(sink);
		reader.run();
		
		new NetworkWriter(sink.getNetwork()).write("output/wurst.xml");
	}

}
