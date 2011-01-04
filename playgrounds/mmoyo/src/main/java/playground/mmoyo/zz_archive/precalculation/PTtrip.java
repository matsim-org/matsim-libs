package playground.mmoyo.zz_archive.precalculation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**Describes simply a trip in a pt-vechicle*/
public class PTtrip {
	//-> use leg with infinite time values instead?
	private TransitRoute transitRoute;
	private NetworkRoute subRoute;
	private double travelTime;
	private final Network network;

	public PTtrip(final TransitRoute transitRoute, final NetworkRoute subRoute, final double travelTime, final Network network) {
		this.transitRoute = transitRoute;
		this.subRoute = subRoute;
		this.travelTime= travelTime;
		this.network = network;
	}

	public TransitRoute getTransitRoute() {
		return transitRoute;
	}
	public NetworkRoute getRoute() {
		return subRoute;
	}

	public double getTravelTime(){
		return travelTime;
	}

	/**Returns the first Facility Id */
	public Id getBoardFacilityId (){
		return this.network.getLinks().get(subRoute.getStartLinkId()).getToNode().getId();
	}


}


