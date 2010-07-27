package playground.mzilske.osm;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;

import uk.co.randomjunk.osmosis.transform.v0_6.TransformTask;

public class OsmMainWithoutDefaults {
	
	public static void main(String[] args) {
		Scenario scenario = new ScenarioImpl();
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("highway", new HashSet<String>(Arrays.asList("motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street")));
		String filename = "inputs/schweiz/zurich.osm";
		Set<String> tagKeys = Collections.emptySet();
		TagFilter tagFilter = new TagFilter("accept-way", tagKeys, tagKeyValues);
		
		FastXmlReader reader = new FastXmlReader(new File(filename ), true, CompressionMethod.None);
		
		SimplifyTask simplify = new SimplifyTask(IdTrackerType.BitSet);
		
		TransformTask tagTransform = new TransformTask("input/schweiz/tagtransform.xml", "output/tagtransform-stats.xml");
		
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);
		NetworkSink sink = new NetworkSink(scenario.getNetwork(), coordinateTransformation);
		
		reader.setSink(tagFilter);
		tagFilter.setSink(simplify);
		simplify.setSink(tagTransform);
		tagTransform.setSink(sink);

		reader.run();
		
		new NetworkWriter(scenario.getNetwork()).write("output/wurst.xml");
	}

}
