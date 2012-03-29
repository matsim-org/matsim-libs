package playground.tnicolai.matsim4opus.utils.network;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.run.NetworkCleaner;

public class OSM2MATSimNetwork2 {
	
	private static final String PATH = "/Users/thomas/Development/opus_home/data/brussels_zone/data/matsim/network/archive/";//"../";
	private static final String INFILE = PATH + "belgium_incl_borderArea.osm";//"belgium_filtered.osm";
	private static final String OUTFILE = PATH + "belgium_incl_borderAreaV2.xml.gz";
	
	public static void main(final String[] args) {

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
		Network network = sc.getNetwork();
		// get transformation for Belgium (EPSG:31300)
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:31300");
		OsmNetworkReader osmReader = new OsmNetworkReader(network, ct);

		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);

		osmReader.setHierarchyLayer(51.671, 2.177, 49.402, 6.764, 2); //belgium and bordering areas
		osmReader.setHierarchyLayer(51.328, 3.639, 50.645, 4.888, 4); //greater brussel area
		osmReader.setHierarchyLayer(50.9515, 4.1748, 50.7312, 4.5909, 5); //city of brussel
		
		osmReader.parse(INFILE);
		new NetworkWriter(network).write(OUTFILE);
		// reads "OUTFILE", cleans it and writes it out as "OUTFILE_clean.xml.gz"
		new NetworkCleaner().run(OUTFILE, OUTFILE.split(".xml")[0] + "_clean.xml.gz");
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(OUTFILE.split(".xml")[0] + "_clean.xml.gz");
		network = (NetworkImpl) scenario.getNetwork();
		
		// network contains "OUTFILE_clean.xml.gz" from above, its simplified and written out as "OUTFILE__clean_simple.xml.gz
		NetworkSimplifier simpl = new NetworkSimplifier();
		Set<Integer> nodeTypess2merge = new HashSet<Integer>();
		nodeTypess2merge.add(new Integer(4));
		nodeTypess2merge.add(new Integer(5));
		simpl.setNodesToMerge(nodeTypess2merge);
		simpl.run(network);
		new NetworkWriter(network).write(OUTFILE.split(".xml")[0] + "_clean_simple.xml.gz");
	}
}
