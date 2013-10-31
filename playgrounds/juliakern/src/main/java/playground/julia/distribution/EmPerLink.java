package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class EmPerLink {
	
	Id linkId;
	Id personId;
	Double value;

	public EmPerLink(Id linkId, Id personId, Double value) {
		this.linkId = linkId;
		this.personId = personId;
		this.value = value;
	}

}
