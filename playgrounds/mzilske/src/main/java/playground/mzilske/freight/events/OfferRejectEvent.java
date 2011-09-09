package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.api.Offer;

public class OfferRejectEvent implements Event{

	private Offer offer;
	
	public OfferRejectEvent(Offer offer) {
		super();
		this.offer = offer;
	}

	public Offer getOffer() {
		return offer;
	}

	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, String> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

}
