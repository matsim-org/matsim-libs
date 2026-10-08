package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;

class BackpackActivity {

	private Activity activity;

	BackpackActivity() {
	}

	BackpackActivity(Msg msg) {
		activity = msg.activity();
	}

	/**
	 * Starts with a copy of the type and location of a planned activity. Start and end times are left undefined, as they are only known
	 * from events.
	 */
	BackpackActivity(Activity plannedActivity) {
		activity = PopulationUtils.createActivityFromLinkId(plannedActivity.getType(), plannedActivity.getLinkId());
		activity.setFacilityId(plannedActivity.getFacilityId());
		activity.setCoord(plannedActivity.getCoord());
	}

	void handleEvent(Event e) {
		if (e instanceof ActivityStartEvent ase) {
			activity = PopulationUtils.createActivityFromLinkId(ase.getActType(), ase.getLinkId());
			activity.setFacilityId(ase.getFacilityId());
			activity.setCoord(ase.getCoord());
			activity.setStartTime(ase.getTime());
		} else if (e instanceof ActivityEndEvent aee) {
			if (activity == null) {
				activity = PopulationUtils.createActivityFromLinkId(aee.getActType(), aee.getLinkId());
				activity.setFacilityId(aee.getFacilityId());
				activity.setCoord(aee.getCoord());
			}
			activity.setEndTime(aee.getTime());
		}
	}

	Activity finishActivity() {
		var result = activity;
		activity = null;
		return result;
	}

	Msg toMessage() {
		return new Msg(activity);
	}

	record Msg(Activity activity) implements Message {
	}
}
