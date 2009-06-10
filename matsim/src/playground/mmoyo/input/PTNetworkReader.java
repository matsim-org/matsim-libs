package playground.mmoyo.input;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.api.network.Network;
import org.matsim.core.network.NetworkLayer;

/**
 * Reads a network and converts its nodes into PTNodes
 */
public class PTNetworkReader{
	
	public PTNetworkReader() {
	
	}

	public Network readNetFile(String inFileName){
		NetworkFactory networkFactory = new NetworkFactory();
	
		Network tempNet= new NetworkLayer(networkFactory);
		Network ptNetworkLayer= new NetworkLayer();
		
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(tempNet);
		matsimNetworkReader.readFile(inFileName);
		
		PTNodeFactory ptNodeFactory = new PTNodeFactory();
		ptNodeFactory.transformToPTNodes(tempNet, ptNetworkLayer);
	
		tempNet= null;
		networkFactory= null;
		matsimNetworkReader= null;
		return ptNetworkLayer;
	}
}
