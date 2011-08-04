package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface TollCalculator {

	public double getToll(Id linkId);

}
