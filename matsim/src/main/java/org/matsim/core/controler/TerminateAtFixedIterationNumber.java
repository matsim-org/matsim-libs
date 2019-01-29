package org.matsim.core.controler;

import org.matsim.core.config.groups.ControlerConfigGroup;

import javax.inject.Inject;

class TerminateAtFixedIterationNumber implements TerminationCriterion {

	private final int lastIteration;

	@Inject
	TerminateAtFixedIterationNumber(ControlerConfigGroup controlerConfigGroup) {
		this.lastIteration = controlerConfigGroup.getLastIteration();
	}

	@Override
	public boolean continueIterations(int iteration) {
		return (iteration <= lastIteration);
	}

}
