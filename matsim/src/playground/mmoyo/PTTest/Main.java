package playground.mmoyo.PTTest;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;

import playground.mmoyo.PTCase2.PTActWriter;

public class Main {
	private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/"; 
	private static final String LOGICPTNETFILE= PATH + "network.xml";
	private static final String OUTPUTPLANS= PATH + "output_plans.xml";
	private static final String ZURICHPTPLANS= PATH + "plans.xml";
	
	NetworkLayer logicNet;
	
	/**
	 * @param args
	 */
	public void main(String[] args) {
		//PTActWriter ptActWriter = new PTActWriter(pt);
		//ptActWriter.findRouteForActivities();
	}
	
	/**
	 * Reads a transitSchedule and creates a network with 
	 * nodes with affixes
	 * transfer links
	 * detached transfers
	 */
	private NetworkLayer createLogicNet(final String inputFile){
		NetworkLayer net = readNetwork(inputFile);
		
		/**Read the transitSchedule file */
		//->TODO
		
		/**Create affixed nodes  */
		//-> TODO
		
		/**Create transfer links */
		//-> TODO
		
		/**Create detached transfers*/
		//-> TODO
		
		return net;
	}
	
	
	private static NetworkLayer readNetwork(final String filePath){
		NetworkLayer net= new NetworkLayer(new NetworkFactory());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(net);
		matsimNetworkReader.readFile(filePath);
		return net;
	}
	
	

}
