package playground.mzilske.d4d;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.io.FileNotFoundException;

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
		OsmNetworkReader sink = new OsmNetworkReader(scenario.getNetwork(), D4DConsts.ct);
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
		sink.parse(D4DConsts.D4D_DIR + "ivory-coast-latest.osm.pbf");
		return scenario;
	}

}
