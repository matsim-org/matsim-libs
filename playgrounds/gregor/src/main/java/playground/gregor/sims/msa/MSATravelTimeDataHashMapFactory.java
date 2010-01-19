package playground.gregor.sims.msa;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeData;
import org.matsim.core.trafficmonitoring.TravelTimeDataFactory;

public class MSATravelTimeDataHashMapFactory implements TravelTimeDataFactory {

	
	private final Network network;
	private final int binSize;

	public MSATravelTimeDataHashMapFactory(Network network, int binSize) {
		this.network = network;
		this.binSize = binSize;
	}

	@Override
	public TravelTimeData createTravelTimeData(Id linkId) {
		return new MSATravelTimeDataHashMap(this.network.getLinks().get(linkId),this.binSize);
	}

}
