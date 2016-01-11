package playground.smetzler.bike;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.io.OsmNetworkReader.OsmNode;
import org.matsim.core.utils.io.OsmNetworkReader.OsmWay;

public class OsmBikeNetworkReader extends OsmNetworkReader {

	public OsmBikeNetworkReader(Network network, CoordinateTransformation transformation) {
		super(network, transformation);
		// TODO Auto-generated constructor stub
		

	}


	private void createBikeLink(final Network network, final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length) {
		
	};
	
}

