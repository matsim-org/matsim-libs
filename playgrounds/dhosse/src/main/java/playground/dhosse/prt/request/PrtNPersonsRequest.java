package playground.dhosse.prt.request;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.RequestImpl;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

public class PrtNPersonsRequest extends RequestImpl implements PassengerRequest {
	
	private final List<MobsimPassengerAgent> passengers;
	private final Link fromLink;
	private final Link toLink;
	private double t0;
	private double t1;
	
	public PrtNPersonsRequest(Id<Request> id, List<MobsimPassengerAgent> passengers, Link fromLink, Link toLink, double quantity, double t0, double t1,
			double submissionTime) {
		super(id, quantity, t0, t1, submissionTime);
		this.passengers = passengers;
		this.fromLink = fromLink;
		this.toLink = toLink;
		this.t0 = t0;
		this.t1 = t1;
	}

	public List<MobsimPassengerAgent> getPassengers(){
		return this.passengers;
	}

	@Override
	public Link getFromLink() {
		return this.fromLink;
	}

	@Override
	public Link getToLink() {
		return this.toLink;
	}

	@Override
	public MobsimPassengerAgent getPassenger() {
		return this.passengers.get(0);
	}

}
