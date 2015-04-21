package playground.mzilske.d4d;

import crosby.binary.osmosis.OsmosisReader;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.tagfilter.v0_6.TagFilter;
import playground.mzilske.osm.NetworkSink;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class CreateNetwork {
	
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = createScenario();
		new NetworkCleaner().run(scenario.getNetwork());
		new NetworkWriter(scenario.getNetwork()).write(D4DConsts.WORK_DIR + "network.xml");
		new NetworkSimplifier().run(scenario.getNetwork());
		new NetworkWriter(scenario.getNetwork()).write(D4DConsts.WORK_DIR + "network-simplified.xml");
	}

	private static Scenario createScenario() throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		
		tagKeyValues.put("highway", new HashSet<String>(Arrays.asList("motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street")));
		
		
		Set<String> tagKeys = Collections.emptySet();
		TagFilter tagFilter = new TagFilter("accept-way", tagKeys, tagKeyValues);
		
		OsmosisReader reader = new OsmosisReader(new FileInputStream(D4DConsts.D4D_DIR + "ivory-coast-latest.osm.pbf"));
		
		
		NetworkSink sink = new NetworkSink(scenario.getNetwork(), D4DConsts.ct);
		sink.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		sink.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		sink.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000, false);
		sink.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500, false);
		sink.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500, false);
		sink.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500, false);
		sink.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000, false);
		sink.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300, false);

		reader.setSink(tagFilter);
		tagFilter.setSink(sink);
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
			public void initialize(Map<String, Object> arg0) {
				
			}
			
		});

		reader.run();
		return scenario;
	}

}
