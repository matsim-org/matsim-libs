package org.matsim.contrib.matsim4urbansim.utils.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.tagfilter.v0_6.TagFilter;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.FastXmlReader;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OsmMain {
	/**
	 * Use osmosis first to (i) extract a bounding box or merge several osm networks ...
	 * --rx file=/Users/thomas/Downloads/belgium.osm --bounding-box top=51.13 left=3.89 bottom=50.55 right=5.21 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link --used-node --wx /Users/thomas/Downloads/belgiumReduced.osm
	 * 
	 * If needed use this to convert osm into matsim format, in addition you can set the right projection ...
	 */
	private static String inputOSMNetwork = "/Users/thomas/Development/opus_home/data/brussels_zone/data/matsim/network/archive/belgium_incl_borderArea.osm";// "/Users/thomas/Downloads/belgium.osm";
	private static String outputMATSimNetwork = "/Users/thomas/Development/opus_home/data/brussels_zone/data/matsim/network/archive/belgium_incl_borderAreaV2.xml.gz";
	
	public static void main(String[] args) {
		Scenario scenario = createScenario();
		new NetworkCleaner().run(scenario.getNetwork());
		new NetworkWriter(scenario.getNetwork()).write(outputMATSimNetwork);
	}

	private static Scenario createScenario() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Map<String, Set<String>> tagKeyValues = new ConcurrentHashMap<String, Set<String>>();
		tagKeyValues.put("highway", new HashSet<String>(Arrays.asList("motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street")));
		Set<String> tagKeys = Collections.emptySet();
		TagFilter tagFilter = new TagFilter("accept-way", tagKeys, tagKeyValues);
		
		FastXmlReader reader = new FastXmlReader(new File(inputOSMNetwork ), true, CompressionMethod.None);
		
		SimplifyTask simplify = new SimplifyTask(IdTrackerType.BitSet);
		
		GeotoolsTransformation gt = new GeotoolsTransformation(TransformationFactory.WGS84, "EPSG:31300");
		// CoordinateTransformation coordinateTransformation = new WGS84ToMercator.Project(18);
		// NetworkSink sink = new NetworkSink(scenario.getNetwork(), coordinateTransformation);
		NetworkSink sink = new NetworkSink(scenario.getNetwork(), gt);
		sink.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		sink.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		sink.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000, true);
		sink.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500, true);
		sink.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500, true);
		sink.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500, true);
		sink.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000, true);
		sink.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600, true);
		sink.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600, true);
		sink.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300, false);

		reader.setSink(tagFilter);
		tagFilter.setSink(simplify);
		simplify.setSink(sink);
		sink.setSink(new Sink() {

			@Override
			public void process(EntityContainer entityContainer) {
				
			}

			@Override
			public void complete() {
				
			}

			@Override
			public void release() {
				
			}

			@Override
			public void initialize(Map<String, Object> metaData) {
				// TODO Auto-generated method stub
				
			}
			
		});

		reader.run();
		return scenario;
	}

}
