package playground.mmoyo.precalculation;

import org.matsim.api.core.v01.network.Node;

public class DynamicConnection {

	private StaticConnection staticConnection;
	private double departureTime;
	
	public DynamicConnection(final StaticConnection staticConnection) {
		this.staticConnection = staticConnection;
	}
	
	/**looks for a precalculated connection with a given time and with a desired number of transfers*/
	public void findConnections (Node node1, Node node2, double departureTime, int transfers){
	
		
	}
	
	



}

