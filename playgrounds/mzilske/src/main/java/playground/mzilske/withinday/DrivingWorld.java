package playground.mzilske.withinday;

import org.matsim.api.core.v01.Id;

public interface DrivingWorld {

	void park();

	void nextTurn(Id poll);

	boolean requiresAction();

}
