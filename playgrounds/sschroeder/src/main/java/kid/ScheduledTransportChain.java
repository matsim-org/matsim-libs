package kid;

import java.util.List;

public class ScheduledTransportChain {
	
	private TransportChain transportChain;
	
	private List<TransportLeg> transportLegs;

	public ScheduledTransportChain(TransportChain transportChain,
			List<TransportLeg> transportLegs) {
		super();
		this.transportChain = transportChain;
		this.transportLegs = transportLegs;
	}

	public TransportChain getTransportChain() {
		return transportChain;
	}

	public List<TransportLeg> getTransportLegs() {
		return transportLegs;
	}
	
}
