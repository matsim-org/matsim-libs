package playground.mzilske.osm;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;

public class OsmTransitMain {
	
	public static void main(String[] args) throws IOException {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);
		String filename = "inputs/schweiz/zurich-filtered.osm";
		FastXmlReader reader = new FastXmlReader(new File(filename ), true, CompressionMethod.None);		
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
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
		reader.setSink(networkGenerator);
		networkGenerator.setSink(transitNetworkSink);
		reader.run();
		new NetworkWriter(scenario.getNetwork()).write("output/transit-network.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("output/transit.xml");
	}

}
