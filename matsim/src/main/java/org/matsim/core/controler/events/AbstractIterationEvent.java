package org.matsim.core.controler.events;

import org.matsim.core.controler.MatsimServices;

public class AbstractIterationEvent extends ControlerEvent {
	private final int iteration;
	private final boolean isLastIteration;

	public AbstractIterationEvent(MatsimServices services, int iteration, boolean isLastIteration) {
		super(services);

		this.iteration = iteration;
		this.isLastIteration = isLastIteration;
	}

	public int getIteration() {
		return iteration;
	}

	public boolean isLastIteration() {
		return isLastIteration;
	}
}
