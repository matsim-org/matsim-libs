package org.matsim.analysis;


import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scoring.EventsToScore;

import javax.inject.Inject;

class TravelDistanceStatsControlerListener implements IterationEndsListener, ShutdownListener {

	@Inject
	private EventsToScore eventsToScore;

	@Inject
	private TravelDistanceStats travelDistanceStats;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		travelDistanceStats.addIteration(event.getIteration(), eventsToScore.getAgentRecords());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		travelDistanceStats.close();
	}
}
