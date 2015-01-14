package playground.dhosse.prt.request;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import playground.michalm.taxi.data.TaxiRequest;

public class PrtRequestCreator implements PassengerRequestCreator {

	public static final String MODE = "prt";
	
	@Override
	public PassengerRequest createRequest(Id<Request> id,
			MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double t0, double t1, double now) {
		return new TaxiRequest(id, passenger, fromLink, toLink, t0, now);
	}
	
}
