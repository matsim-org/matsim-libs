package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

public class TSPCapabilities {
	private List<Id> transshipmentCentres = new ArrayList<Id>();

	public List<Id> getTransshipmentCentres() {
		return transshipmentCentres;
	}
}
