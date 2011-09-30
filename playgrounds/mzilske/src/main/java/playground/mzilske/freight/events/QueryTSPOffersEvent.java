package playground.mzilske.freight.events;

import java.util.Collection;
import java.util.Map;

import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.api.Offer;

public class QueryTSPOffersEvent implements Event{

	private Service service;
	
	private Collection<Offer> offers;
	
	public Service getService() {
		return service;
	}

	public Collection<Offer> getOffers() {
		return offers;
	}

	public QueryTSPOffersEvent(Collection<Offer> offers,Service service) {
		super();
		this.service = service;
		this.offers = offers;
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
