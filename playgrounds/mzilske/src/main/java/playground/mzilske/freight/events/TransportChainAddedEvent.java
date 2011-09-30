package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.TransportChain;

public class TransportChainAddedEvent implements Event{

private Id tspId;
	
	private TransportChain chain;
	
	public TransportChainAddedEvent(Id tspId, TransportChain chain) {
		super();
		this.tspId = tspId;
		this.chain = chain;
	}

	public Id getTspId() {
		return tspId;
	}

	public TransportChain getChain() {
		return chain;
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
