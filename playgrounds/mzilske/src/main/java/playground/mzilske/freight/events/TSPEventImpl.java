package playground.mzilske.freight.events;

import org.matsim.api.core.v01.Id;

public class TSPEventImpl {
	
	private Id tspId;

	public TSPEventImpl(Id tspId) {
		super();
		this.tspId = tspId;
	}

	public Id getTspId() {
		return tspId;
	}
}
