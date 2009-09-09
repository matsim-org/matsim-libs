package playground.mmoyo.deprecVersion;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkFactory;

/**
 * Reads a network and converts its nodes into PTNodes
 */
public class PTNetworkReader{
	
	public PTNetworkReader() {
	
	}

	public NetworkLayer readNetFile(final String inFileName){
		NetworkFactory networkFactory = new NetworkFactory();
	
		NetworkLayer tempNet= new NetworkLayer(networkFactory);
		NetworkLayer ptNetworkLayer= new NetworkLayer();
		
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
