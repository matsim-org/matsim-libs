package playground.mzilske.freight;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;

public class TSPKnowledge {
	
	private Set<Id> knownCarriers = new HashSet<Id>();

	Set<Id> getKnownCarriers() {
		return knownCarriers;
	}

}
