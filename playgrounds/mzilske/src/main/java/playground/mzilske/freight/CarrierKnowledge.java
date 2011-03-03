package playground.mzilske.freight;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;

public class CarrierKnowledge {
	private Set<Id> noGoLocations = new HashSet<Id>();

	public Set<Id> getNoGoLocations() {
		return noGoLocations;
	}
	
	
}
