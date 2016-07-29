package besttimeresponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PlannedActivitySequence {

	private final List<PlannedActivity> activities = new ArrayList<>();

	public PlannedActivitySequence() {
	}

	public List<PlannedActivity> entries() {
		return this.activities;
	}

	public PlannedActivity getLast() {
		return this.activities.get(this.activities.size() - 1);
	}
}
