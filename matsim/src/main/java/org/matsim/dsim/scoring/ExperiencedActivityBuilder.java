package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;

public class ExperiencedActivityBuilder {

	private Activity activity;

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
}
