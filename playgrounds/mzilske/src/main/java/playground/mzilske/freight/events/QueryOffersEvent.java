package playground.mzilske.freight.events;

import java.util.Collection;
import java.util.Map;

import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.api.Offer;

public class QueryOffersEvent implements Event{

	private Collection<Offer> offers;
	
	private Service service;
	
	public QueryOffersEvent(Collection<Offer> offers, Service service) {
		super();
		this.offers = offers;
		this.service = service;
	}
	
	public Collection<Offer> getOffers() {
		return offers;
	}

	public Service getService() {
		return service;
	}

	@Override
	public double getTime() {
		return 0;
	}

	@Override
	public Map<String, String> getAttributes() {
		return null;
	}

}
