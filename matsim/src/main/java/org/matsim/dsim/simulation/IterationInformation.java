package org.matsim.dsim.simulation;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

public class IterationInformation implements IterationStartsListener {

	private int iteration;

	public int iteration() {
		return iteration;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.iteration = event.getIteration();
	}
}
