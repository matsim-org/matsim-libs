package org.matsim.analysis;


import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scoring.ExperiencedPlansService;

import javax.inject.Inject;

class TravelDistanceStatsControlerListener implements IterationEndsListener, ShutdownListener {

	@Inject
	private ExperiencedPlansService experiencedPlansService;

	@Inject
	private TravelDistanceStats travelDistanceStats;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		travelDistanceStats.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		travelDistanceStats.close();
	}
}
