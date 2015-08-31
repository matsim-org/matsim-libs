package playground.dhosse.gap.scenario.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.io.OsmNetworkReader;

import playground.dhosse.gap.scenario.GAPMain;

public class NetworkCreator {

	public static void createAndAddNetwork(Scenario scenario, String osmFile){
		
		Network network = scenario.getNetwork();
		
		OsmNetworkReader onr = new OsmNetworkReader(network, GAPMain.ct);
		
		onr.setHierarchyLayer(48.2430, 9.6268, 46.6645, 12.4365, 4);
		onr.setHierarchyLayer(47.7389, 10.8662, 47.3793, 11.4251, 6); //complete ways of lk garmisch-partenkirchen
		onr.setHierarchyLayer(47.4330, 11.1034, 47.2871, 11.2788, 6); //complete ways of seefeld & leutasch
		onr.setKeepPaths(true);
		
		onr.parse(osmFile);
		
		new NetworkCleaner().run(network);
		
	}
	
}
