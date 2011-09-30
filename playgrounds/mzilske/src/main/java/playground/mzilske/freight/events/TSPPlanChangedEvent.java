package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.TransportChain;


public class TSPPlanChangedEvent implements Event{

	private Id tspId;
	
	private TransportChain oldChain;
	
	private TransportChain newChain;
	
	public TSPPlanChangedEvent(Id tspId, TransportChain oldChain, TransportChain newChain) {
		super();
		this.tspId = tspId;
		this.oldChain = oldChain;
		this.newChain = newChain;
	}
	
	public Id getTspId() {
		return tspId;
	}

	public TransportChain getOldChain() {
		return oldChain;
	}

	public TransportChain getNewChain() {
		return newChain;
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
