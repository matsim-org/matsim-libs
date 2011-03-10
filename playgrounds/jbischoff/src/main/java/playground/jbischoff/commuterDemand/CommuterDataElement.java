package playground.jbischoff.commuterDemand;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class CommuterDataElement {
	private Id fromId;
	private Id toId;
	private int commuters;
	
	public CommuterDataElement(String from, String to, int commuters){
		this.fromId = new IdImpl(from);
		this.toId = new IdImpl(to);
		this.commuters = commuters;
	}

	public Id getFromId() {
		return fromId;
	}

	public Id getToId() {
		return toId;
	}

	public int getCommuters() {
		return commuters;
	}
	
	public String toString(){
		return ("F: "+fromId+" T: "+toId+" C: "+commuters);
		
	}

}
