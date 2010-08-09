package playground.mzilske.osm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;

public class OsmTransitMain {
	
	public static void main(String[] args) throws IOException {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);
		String filename = "inputs/schweiz/zurich.osm";
		FastXmlReader reader = new FastXmlReader(new File(filename ), true, CompressionMethod.None);		
		
		// TransformTask transform = new TransformTask("inputs/schweiz/zurich-transit-transform.xml", "output/stats.xml");
		
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("route", new HashSet<String>(Arrays.asList("tram", "train", "bus")));
		Set<String> tagKeys = Collections.emptySet();
		TagFilter transitFilter = new TagFilter("accept-relation", tagKeys, tagKeyValues);
		
		UsedNodeAndWayFilter usedFilter = new UsedNodeAndWayFilter(IdTrackerType.BitSet);
		
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);
		NetworkSink networkGenerator = new NetworkSink(scenario.getNetwork(), coordinateTransformation);
		networkGenerator.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		networkGenerator.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		networkGenerator.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000, false);
		networkGenerator.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500, false);
		networkGenerator.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500, false);
		networkGenerator.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500, false);
		networkGenerator.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000, false);
		networkGenerator.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600, false);
		networkGenerator.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600, false);
		networkGenerator.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600, false);
		networkGenerator.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600, false);
		networkGenerator.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300, false);
		
		TransitNetworkSink transitNetworkSink = new TransitNetworkSink(scenario.getNetwork(), scenario.getTransitSchedule(), coordinateTransformation, IdTrackerType.BitSet);
		
		
		reader.setSink(transitFilter);
		// transform.setSink(transitFilter);
		transitFilter.setSink(usedFilter);
		usedFilter.setSink(networkGenerator);
		networkGenerator.setSink(transitNetworkSink);
		reader.run();
		new NetworkWriter(scenario.getNetwork()).write("output/transit-network.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("output/transit.xml");
	}

}
