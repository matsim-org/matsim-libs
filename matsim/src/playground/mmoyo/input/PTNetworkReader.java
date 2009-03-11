package playground.mmoyo.input;

import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;

public class PTNetworkReader{
	
	public PTNetworkReader() {
	
	}

	public NetworkLayer readNetFile(String inFileName){
		NetworkFactory networkFactory = new NetworkFactory();
	
		NetworkLayer tempNet= new NetworkLayer(networkFactory);
		NetworkLayer ptNetworkLayer= new NetworkLayer();
		
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(tempNet);
		matsimNetworkReader.readFile(inFileName);
		
		PTNodeFactory ptNodeFactory = new PTNodeFactory();
		ptNodeFactory.TransformToPTNodes(tempNet, ptNetworkLayer);
	
		
		
		tempNet= null;
		networkFactory= null;
		matsimNetworkReader= null;
		return ptNetworkLayer;
	}
}
