package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;
import org.matsim.modechoice.PlanModel;

import java.util.List;

/**
 * Complex estimator, using MATSim infrastructure
 */
public final class ComplexTripEstimator implements TripEstimator, LinkEnterEventHandler, IterationEndsListener {

	private int iters = 0;
	private int events = 0;

	@Override
	public MinMaxEstimate estimate(EstimatorContext context, String mode, PlanModel plan, List<Leg> trip, ModeAvailability option) {
		return MinMaxEstimate.ofMax(10);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		events++;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		iters++;
	}

	public int getEvents() {
		return events;
	}

	public int getIters() {
		return iters;
	}
}
