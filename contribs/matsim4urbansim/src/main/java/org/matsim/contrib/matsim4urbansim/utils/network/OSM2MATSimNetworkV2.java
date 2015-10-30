package org.matsim.contrib.matsim4urbansim.utils.network;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.run.NetworkCleaner;

public class OSM2MATSimNetworkV2 {
	
	private static final String PATH = "/Users/thomas/Development/opus_home/data/brussels_zone/data/matsim/network/archive/";//"../";
	private static final String INFILE = PATH + "belgium_incl_borderArea.osm";//"belgium_filtered.osm";
	private static final String OUTFILE = PATH + "belgium_incl_borderArea_hierarchylayer4.xml.gz";
	
	public static void main(final String[] args) {
		
		// this is a simple version of the code below
//		String osm = "/Users/thomas/Development/opus_home/data/brussels_zone/data/matsim/network/archive/belgium_incl_borderArea.osm";
//		Config config = ConfigUtils.createConfig();
//		Scenario sc = ScenarioUtils.createScenario(config);
//		Network net = sc.getNetwork();
//		// tnicolai: get name from projection identifiyer (EPSG)
//		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:31300");
//		String transformation = crs.getName().toString();
//		
//		//CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);
//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, transformation);
//		OsmNetworkReader onr = new OsmNetworkReader(net, ct);
//		onr.parse(osm);
//		new NetworkCleaner().run(net);
//		new NetworkWriter(net).write("/Users/thomas/Downloads/merged-network.xml");

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// creating an empty matsim network
		Network network = sc.getNetwork();
		// using the Belge Lambert 72 projection for the matsim network
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84,
						"EPSG:31300");
		OsmNetworkReader osmReader = new OsmNetworkReader(network, ct);

		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);

		// this layer covers the whole area, Belgium and bordering areas
		// including OSM motorways and trunks
		// osmReader.setHierarchyLayer(51.671, 2.177, 49.402, 6.764, 2);
		osmReader.setHierarchyLayer(51.671, 2.177, 49.402, 6.764, 4);
		// this layer covers the greater Brussels area including
		// OSM secondary roads or greater
		// osmReader.setHierarchyLayer(51.328, 3.639, 50.645, 4.888, 4); // tnicolai: old layer extend too small
		// osmReader.setHierarchyLayer(51.4, 3.55, 50.3, 5.5, 5);
		
		// tnicolai: this layer is removed, because of having different layers within the
		//			 the study area that cause different network densities !!!
		// this layer covers the city of Brussels including OSM 
		// tertiary roads or greater
		// osmReader.setHierarchyLayer(50.9515, 4.1748, 50.7312, 4.5909, 5);

		// converting the merged OSM network into matsim format
		osmReader.parse(INFILE);
		new NetworkWriter(network).write(OUTFILE);
		// writing out a cleaned matsim network and loading it 
		// into the scenario
		new NetworkCleaner().run(OUTFILE, OUTFILE.split(".xml")[0]
				+ "_clean.xml.gz");
		Scenario scenario = (MutableScenario) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(OUTFILE.split(".xml")[0]
				+ "_clean.xml.gz");
		network = (NetworkImpl) scenario.getNetwork();

		// simplifying the cleaned network
		NetworkSimplifier simplifier = new NetworkSimplifier();
		Set<Integer> nodeTypess2merge = new HashSet<Integer>();
		nodeTypess2merge.add(new Integer(4));
		nodeTypess2merge.add(new Integer(5));
		simplifier.setNodesToMerge(nodeTypess2merge);
		simplifier.run(network);
		new NetworkWriter(network).write(OUTFILE.split(".xml")[0]
				+ "_clean_simple.xml.gz");
	}
}
